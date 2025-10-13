
package com.relative.chat.bot.ia.application.ports.out;

import java.util.List;

public interface EmbeddingsPort {
    String model();

    float[] embedOne(String text);

    List<float[]> embedMany(List<String> texts);

}
