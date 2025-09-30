
package com.relative.chat.bot.ia.infrastructure.migration;
import com.relative.chat.bot.ia.application.usecases.MigrateVectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
@Component @Profile("migrate") @RequiredArgsConstructor
public class MigrationRunner implements CommandLineRunner {
    private final MigrateVectors migrate;
    @Override public void run(String... args) {
        String nsSource = System.getProperty("source.ns", "kb");
        String nsDest   = System.getProperty("dest.ns",   "kb2");
        int dim         = Integer.getInteger("vector.dim", 1536);
        int batch       = Integer.getInteger("batch.size", 500);
        long total = migrate.handle(nsSource, nsDest, dim, batch);
        System.out.println("[migration] done, total=" + total);
    }
}
