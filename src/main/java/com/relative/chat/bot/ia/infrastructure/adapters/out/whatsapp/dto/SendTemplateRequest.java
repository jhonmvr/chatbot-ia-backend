package com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp.dto;

import java.util.Map;

public record SendTemplateRequest(String from, String to, String templateId, Map<String, String> parameters) {}
