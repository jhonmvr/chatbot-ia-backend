package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.application.ports.out.EmbeddingsPort;
import com.relative.chat.bot.ia.application.ports.out.VectorStore;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.knowledge.Kb;
import com.relative.chat.bot.ia.domain.knowledge.KbChunk;
import com.relative.chat.bot.ia.domain.knowledge.KbDocument;
import com.relative.chat.bot.ia.domain.model.Document;
import com.relative.chat.bot.ia.domain.ports.knowledge.KbRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Use Case: Ingestar documentos en el Knowledge Base
 * 
 * Proceso:
 * 1. Crear registros de documentos en la BD
 * 2. Dividir documentos en chunks
 * 3. Generar embeddings para cada chunk
 * 4. Persistir embeddings y referencias en vector store
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class IngestDocuments {
    
    private final EmbeddingsPort embeddings;
    private final VectorStore vectorStore;
    private final KbRepository kbRepository;
    
    @Transactional
    public void handle(String namespace, List<Document> docs) {
        log.info("Iniciando ingesta de {} documentos en namespace '{}'", docs.size(), namespace);
        
        // Extraer kbId del namespace (formato: "kb_<uuid>")
        UUID kbId = extractKbIdFromNamespace(namespace);
        if (kbId == null) {
            throw new IllegalArgumentException("Namespace inválido. Debe tener formato: kb_<uuid>");
        }
        
        // Verificar que el KB existe
        Optional<Kb> kbOpt = kbRepository.findById(UuidId.of(kbId));
        if (kbOpt.isEmpty()) {
            throw new IllegalArgumentException("Knowledge Base no encontrado: " + kbId);
        }
        
        Kb kb = kbOpt.get();
        log.info("Ingesta en KB: {} (Cliente: {})", kb.name(), kb.clientId().value());
        
        // Listas para los vectores y records
        List<String> textsToEmbed = new ArrayList<>();
        List<KbChunk> chunks = new ArrayList<>();
        
        // Para cada documento, crear estructura en BD
        for (Document doc : docs) {
            // 1. Crear KbDocument
            KbDocument kbDoc = new KbDocument(
                UuidId.of(UUID.randomUUID()),
                UuidId.of(kbId),
                doc.metadata().getOrDefault("source", "unknown").toString(),
                doc.id(),
                doc.metadata().getOrDefault("filename", "document.txt").toString(),
                doc.metadata().getOrDefault("mimeType", "text/plain").toString(),
                (long) doc.text().length(),
                null,
                doc.metadata().getOrDefault("language", "es").toString()
            );
            
            kbRepository.saveDocument(kbDoc);
            log.debug("Documento creado: {}", kbDoc.id().value());
            
            // 2. Dividir en chunks (por ahora, 1 documento = 1 chunk)
            // TODO: Implementar chunking más inteligente (por tamaño, párrafos, etc.)
            KbChunk chunk = new KbChunk(
                UuidId.of(UUID.randomUUID()),
                kbDoc.id(),
                0, // index
                doc.text(),
                null // tokens (calcularlo si es necesario)
            );
            
            kbRepository.saveChunk(chunk);
            log.debug("Chunk creado: {} para documento {}", chunk.id().value(), kbDoc.id().value());
            
            chunks.add(chunk);
            textsToEmbed.add(doc.text());
        }
        
        // 3. Generar embeddings en batch
        log.info("Generando {} embeddings...", textsToEmbed.size());
        List<float[]> vectors = embeddings.embedMany(textsToEmbed);
        
        // 4. Crear VectorRecords con metadata completa
        List<VectorStore.VectorRecord> records = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            KbChunk chunk = chunks.get(i);
            float[] vector = vectors.get(i);
            
            // Metadata enriquecida para búsqueda
            Map<String, Object> metadata = new HashMap<>(docs.get(i).metadata());
            metadata.put("chunk_id", chunk.id().value().toString());
            metadata.put("document_id", chunk.documentId().value().toString());
            metadata.put("kb_id", kbId.toString());
            metadata.put("client_id", kb.clientId().value().toString());
            metadata.put("text", chunk.content());
            metadata.put("chunk_index", chunk.index());
            
            records.add(new VectorStore.VectorRecord(
                chunk.id().value().toString(),
                vector,
                metadata
            ));
        }
        
        // 5. Persistir en vector store (esto creará los embeddings y referencias)
        vectorStore.upsert(namespace, records);
        
        log.info("✅ Ingesta completada: {} documentos, {} chunks, {} embeddings", 
                 docs.size(), chunks.size(), vectors.size());
    }
    
    /**
     * Extrae el UUID del KB desde el namespace
     * Ejemplo: "kb_123e4567-e89b-12d3-a456-426614174000" -> UUID
     */
    private UUID extractKbIdFromNamespace(String namespace) {
        if (namespace == null || !namespace.startsWith("kb_")) {
            return null;
        }
        
        try {
            String uuidPart = namespace.substring(3); // Remover "kb_"
            return UUID.fromString(uuidPart);
        } catch (IllegalArgumentException e) {
            log.error("Error al extraer UUID del namespace: {}", namespace, e);
            return null;
        }
    }
}
