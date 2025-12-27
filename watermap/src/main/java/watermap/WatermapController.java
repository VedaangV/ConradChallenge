package watermap;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WatermapController {

    private static final String DATA_FOLDER = "./data";
    private static final String PIXELS_FILE = "pixels.json";

    @GetMapping("/generatePixels")
    public ResponseEntity<String> generatePixels() throws Exception {
        // Manually trigger pixel generation
        WatermapProcessor processor = new WatermapProcessor();
        processor.run();  // generates pixels.json in ./data folder
        return ResponseEntity.ok("Pixels generated");
    }

    @GetMapping("/data/pixels.json")
    public ResponseEntity<Resource> getPixelsJson() throws Exception {
        File folder = new File(DATA_FOLDER);
        if (!folder.exists()) folder.mkdirs();

        File jsonFile = new File(folder, PIXELS_FILE);

        // Regenerate JSON if it doesn't exist or is older than 1 hour
        if (!jsonFile.exists() || Instant.now().toEpochMilli() - jsonFile.lastModified() > 3600_000) {
            WatermapProcessor processor = new WatermapProcessor();
            processor.run();
        }

        if (!jsonFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = jsonFile.toPath();
        Resource resource = new UrlResource(filePath.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resource);
    }
}
