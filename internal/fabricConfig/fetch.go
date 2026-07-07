package fabricConfig

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
)

const defaultFabricConfigURL = "http://upsilon/upsilon-config/config.yml"

func fabricConfigURL() string {
	if url := os.Getenv("UPSILON_FABRIC_CONFIG_URL"); url != "" {
		return url
	}
	return defaultFabricConfigURL
}

func FetchConfigFromHTTP(destPath string) error {
	url := fabricConfigURL()

	resp, err := http.Get(url)
	if err != nil {
		return fmt.Errorf("fetch fabric config from %s: %w", url, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("fetch fabric config from %s: HTTP %d", url, resp.StatusCode)
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return fmt.Errorf("read fabric config body: %w", err)
	}

	if err := os.MkdirAll(destPath, 0755); err != nil {
		return fmt.Errorf("create fabric config directory: %w", err)
	}

	configPath := filepath.Join(destPath, "config.yml")
	if err := os.WriteFile(configPath, body, 0644); err != nil {
		return fmt.Errorf("write fabric config: %w", err)
	}

	return nil
}
