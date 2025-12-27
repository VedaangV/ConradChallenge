package watermap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WatermapController {

    @GetMapping("/generatePixels")
    public ResponseEntity<String> generatePixels() throws Exception {
        // Run your Java logic to generate pixels.json
        WatermapProcessor processor = new WatermapProcessor();
        processor.run();  // generates pixels.json in ./data folder
        return ResponseEntity.ok("Pixels generated");
    }

    @GetMapping("/data/pixels.json")
    public ResponseEntity<Resource> getPixelsJson() throws IOException {
        // Serve pixels.json from ./data folder (matches WatermapProcessor output)
        File jsonFile = new File("./data/pixels.json");
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
