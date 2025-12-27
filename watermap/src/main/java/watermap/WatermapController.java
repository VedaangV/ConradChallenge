package watermap;

import java.util.Map;

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

    @GetMapping(
        value = "/data/pixels.json",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> getPixelsJson() {
        try {
            // Generate pixels IN MEMORY
            Map<String, Object> result = processor.run();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
