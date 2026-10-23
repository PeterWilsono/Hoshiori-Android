package com.hoshiori.ai.brain

/**
 * 动态记忆图谱 - 整合向量检索和知识图谱
 * 完整移植自 Python vector_memory.py
 * 
 * 核心功能：
 * 1. 整合向量检索和知识图谱
 * 2. 联想式检索（向量 + 图谱扩展）
 * 3. 从JSON记忆构建图谱索引
 * 
 * 这是让AI记忆系统从"数据库"升级到"人类大脑"的关键
 */
class DynamicMemoryGraph {
    
    val vectorSystem = VectorMemorySystem()
    val knowledgeGraph = KnowledgeGraph()
    
    /**
     * 添加记忆（同时更新向量和图谱）
     */
    fun addMemory(content: String, memoryType: String = "general", importance: Int = 5): Int {
        // 如果向量系统还没初始化，先用当前内容初始化
        if (!vectorSystem.embedder.hasVocab()) {
            vectorSystem.embedder.fit(listOf(content))
        }
        
        // 添加到向量系统
        val memoryId = vectorSystem.addMemory(content, mapOf(
            "type" to memoryType,
            "importance" to importance
        ))
        
        // 提取知识图谱三元组
        val triples = knowledgeGraph.extractFromText(content)
        for ((entity1, relation, entity2) in triples) {
            knowledgeGraph.addRelation(entity1, relation, entity2)
        }
        
        return memoryId
    }
    
    /**
     * 联想式检索（向量 + 图谱扩展）
     * 
     * 这是核心功能：
     * 1. 先用向量检索找到语义相似的记忆
     * 2. 再用知识图谱找到关联的实体
     * 3. 合并结果，实现多层次的记忆联想
     */
    fun searchWithAssociation(query: String, topK: Int = 5): List<AssociationResult> {
        val results = mutableListOf<AssociationResult>()
        
        // 1. 向量检索
        val vectorResults = vectorSystem.search(query, topK = topK)
        for (result in vectorResults) {
            results.add(AssociationResult(
                content = result.content,
                metadata = result.metadata,
                similarity = result.similarity,
                source = "vector"
            ))
        }
        
        // 2. 图谱扩展
        val queryEntities = knowledgeGraph.extractFromText(query)
        val graphResults = mutableListOf<AssociationResult>()
        
        for ((entity1, _, entity2) in queryEntities) {
            // 查找相关实体
            val related1 = knowledgeGraph.findRelated(entity1, maxDepth = 2)
            val related2 = knowledgeGraph.findRelated(entity2, maxDepth = 2)
            
            for (rel in related1 + related2) {
                graphResults.add(AssociationResult(
                    content = "${rel.entity} (${rel.relation})",
                    metadata = mapOf("type" to "graph_association", "depth" to rel.depth),
                    similarity = 0.5 * rel.weight / rel.depth,
                    source = "graph"
                ))
            }
        }
        
        // 3. 合并结果
        results.addAll(graphResults)
        
        // 去重并排序
        val seen = mutableSetOf<String>()
        val uniqueResults = mutableListOf<AssociationResult>()
        
        for (result in results.sortedByDescending { it.similarity }) {
            if (result.content !in seen) {
                seen.add(result.content)
                uniqueResults.add(result)
            }
        }
        
        return uniqueResults.take(topK)
    }
    
    /**
     * 从JSON记忆构建图谱
     */
    fun buildFromJson(longTermMemory: LongTermMemory) {
        // 构建向量索引
        vectorSystem.updateFromJson(longTermMemory)
        
        // 构建知识图谱
        for (event in longTermMemory.emotionalEvents) {
            val triples = knowledgeGraph.extractFromText(event.content)
            for ((e1, rel, e2) in triples) {
                knowledgeGraph.addRelation(e1, rel, e2)
            }
        }
        
        for (event in longTermMemory.importantEvents) {
            val triples = knowledgeGraph.extractFromText(event.content)
            for ((e1, rel, e2) in triples) {
                knowledgeGraph.addRelation(e1, rel, e2)
            }
        }
    }
    
    /**
     * 获取图谱统计信息
     */
    fun getGraphStats(): GraphStats {
        return GraphStats(
            entityCount = knowledgeGraph.getAllEntities().size,
            relationCount = knowledgeGraph.getAllRelations().size,
            memoryCount = vectorSystem.getMemoryCount()
        )
    }
}

// 数据类
data class AssociationResult(
    val content: String,
    val metadata: Map<String, Any>,
    val similarity: Double,
    val source: String // "vector" or "graph"
)

data class GraphStats(
    val entityCount: Int,
    val relationCount: Int,
    val memoryCount: Int
)
