package com.hoshiori.ai.brain

/**
 * 知识图谱系统 - 存储实体和关系
 * 完整移植自 Python vector_memory.py
 * 
 * 核心功能：
 * 1. 实体识别和存储
 * 2. 关系三元组提取（主语-谓语-宾语）
 * 3. 图谱关系查询（广度优先搜索）
 * 4. 实体关联发现
 */
class KnowledgeGraph {
    
    private val entities = mutableMapOf<Int, Entity>()
    private val relations = mutableListOf<Relation>()
    private val entityIndex = mutableMapOf<String, Int>()
    private var nextId = 0
    
    /**
     * 添加实体
     */
    fun addEntity(name: String, entityType: String, attributes: Map<String, Any> = emptyMap()): Int {
        // 检查是否已存在
        entityIndex[name]?.let { return it }
        
        val entityId = nextId++
        
        entities[entityId] = Entity(
            id = entityId,
            name = name,
            type = entityType,
            attributes = attributes
        )
        
        entityIndex[name] = entityId
        return entityId
    }
    
    /**
     * 添加关系
     */
    fun addRelation(entity1: String, relation: String, entity2: String, weight: Double = 1.0) {
        // 确保实体存在
        if (entity1 !in entityIndex) {
            addEntity(entity1, "unknown")
        }
        if (entity2 !in entityIndex) {
            addEntity(entity2, "unknown")
        }
        
        val e1Id = entityIndex[entity1]!!
        val e2Id = entityIndex[entity2]!!
        
        relations.add(Relation(e1Id, relation, e2Id, weight))
    }
    
    /**
     * 查找相关实体（广度优先搜索）
     */
    fun findRelated(entityName: String, maxDepth: Int = 2): List<RelatedEntity> {
        val entityId = entityIndex[entityName] ?: return emptyList()
        
        val visited = mutableSetOf(entityId)
        val queue = mutableListOf(entityId to 0) // (entity_id, depth)
        val related = mutableListOf<RelatedEntity>()
        
        while (queue.isNotEmpty()) {
            val (currentId, depth) = queue.removeAt(0)
            
            if (depth >= maxDepth) continue
            
            // 查找所有相关关系
            for (relation in relations) {
                when {
                    relation.entity1Id == currentId && relation.entity2Id !in visited -> {
                        visited.add(relation.entity2Id)
                        queue.add(relation.entity2Id to depth + 1)
                        
                        entities[relation.entity2Id]?.let { entity ->
                            related.add(RelatedEntity(
                                entity = entity.name,
                                relation = relation.relation,
                                depth = depth + 1,
                                weight = relation.weight
                            ))
                        }
                    }
                    relation.entity2Id == currentId && relation.entity1Id !in visited -> {
                        visited.add(relation.entity1Id)
                        queue.add(relation.entity1Id to depth + 1)
                        
                        entities[relation.entity1Id]?.let { entity ->
                            related.add(RelatedEntity(
                                entity = entity.name,
                                relation = "被${relation.relation}",
                                depth = depth + 1,
                                weight = relation.weight
                            ))
                        }
                    }
                }
            }
        }
        
        return related
    }
    
    /**
     * 从文本中提取三元组（简化版NER）
     */
    fun extractFromText(text: String): List<Triple<String, String, String>> {
        val triples = mutableListOf<Triple<String, String, String>>()
        
        // 简单的模式匹配
        val patterns = listOf(
            Triple(
                Regex("(.{1,6})(喜欢|爱|讨厌|害怕)(.{1,10})"),
                { m: MatchResult -> Triple(m.groupValues[1].trim(), m.groupValues[2], m.groupValues[3].trim()) },
                Unit
            ),
            Triple(
                Regex("(.{1,6})(是|叫)(.{1,10})"),
                { m: MatchResult -> Triple(m.groupValues[1].trim(), m.groupValues[2], m.groupValues[3].trim()) },
                Unit
            ),
            Triple(
                Regex("(.{1,6})(在|去了|来自)(.{1,10})"),
                { m: MatchResult -> Triple(m.groupValues[1].trim(), m.groupValues[2], m.groupValues[3].trim()) },
                Unit
            ),
            Triple(
                Regex("(.{1,6})(有|拥有|买了)(.{1,10})"),
                { m: MatchResult -> Triple(m.groupValues[1].trim(), m.groupValues[2], m.groupValues[3].trim()) },
                Unit
            )
        )
        
        for ((pattern, extractor, _) in patterns) {
            pattern.findAll(text).forEach { match ->
                try {
                    val triple = extractor(match)
                    // 过滤掉包含标点的结果
                    val punctuation = setOf('，', '。', '！', '？', ',', '.')
                    if (triple.first.isNotEmpty() && 
                        triple.second.isNotEmpty() && 
                        triple.third.isNotEmpty() &&
                        triple.first.none { it in punctuation } &&
                        triple.second.none { it in punctuation } &&
                        triple.third.none { it in punctuation }) {
                        triples.add(triple)
                    }
                } catch (e: Exception) {
                    // 忽略解析错误
                }
            }
        }
        
        return triples
    }
    
    /**
     * 获取所有实体
     */
    fun getAllEntities(): List<Entity> = entities.values.toList()
    
    /**
     * 获取所有关系
     */
    fun getAllRelations(): List<Relation> = relations.toList()
}

// 数据类
data class Entity(
    val id: Int,
    val name: String,
    val type: String,
    val attributes: Map<String, Any>
)

data class Relation(
    val entity1Id: Int,
    val relation: String,
    val entity2Id: Int,
    val weight: Double
)

data class RelatedEntity(
    val entity: String,
    val relation: String,
    val depth: Int,
    val weight: Double
)
