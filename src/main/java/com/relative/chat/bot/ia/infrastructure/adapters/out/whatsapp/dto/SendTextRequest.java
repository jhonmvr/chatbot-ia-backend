package com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp.dto;


public record SendTextRequest(String from, String to, String message) {}