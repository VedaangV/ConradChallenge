package watermap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WatermapController {

    // Allow all origins to call this endpoint (for HTML fetch)
    @CrossOrigin(origins = "*")
    @GetMapping("/generatePixels")
    public ResponseEntity<String> generatePixels() throws Exception {
        // Run your Java logic to generate pixels.json
        WatermapProcessor processor = new WatermapProcessor();
        processor.run();  // generates pixels.json in ./data folder
        return ResponseEntity.ok("Pixels generated");
    }

    // Allow all origins to fetch the JSON
    @CrossOrigin(origins = "*")
    @GetMapping("/data/pixels.json")
    public ResponseEntity<Resource> getPixelsJson() throws IOException {
        // Serve pixels.json from ./data folder
        File jsonFile = new File("./data/pixels.json");
        if (!jsonFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = jsonFile.toPath();
        Resource resource = new UrlResource(filePath.toUri());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"pixels.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resource);
    }
}
