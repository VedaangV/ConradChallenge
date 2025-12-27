package watermap;

import java.io.File;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WatermapController {

    private final WatermapProcessor processor;

    public WatermapController(WatermapProcessor processor) {
        this.processor = processor;
    }

    @GetMapping("/data/pixels.json")
    public ResponseEntity<Resource> getPixelsJson() {
        try {
            processor.run();

            File jsonFile = new File("./data/pixels.json");
            if (!jsonFile.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(jsonFile.toPath().toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace(); // <-- THIS IS CRITICAL
            return ResponseEntity.internalServerError()
                    .body(null);
        }
    }
}
