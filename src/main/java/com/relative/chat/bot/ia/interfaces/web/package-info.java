/**
 * Capa de Interfaces Web - Controladores REST
 * 
 * Esta capa contiene los controladores REST que exponen la funcionalidad
 * del chatbot a través de HTTP/REST APIs.
 * 
 * Controladores:
 * - WhatsAppController: Webhook genérico para WhatsApp
 * - ConversationController: API de gestión de conversaciones
 * - KnowledgeBaseController: API de gestión del Knowledge Base
 * - HealthController: Endpoints de monitoreo y salud
 * 
 * Los webhooks específicos de proveedores están en:
 * infrastructure/adapters/in/web/MetaWhatsAppWebhookController
 */
package com.relative.chat.bot.ia.interfaces.web;

