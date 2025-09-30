
package com.relative.chat.bot.ia.domain.model;
import java.util.Map;
public record Document(String id, String text, Map<String,Object> metadata) {}
