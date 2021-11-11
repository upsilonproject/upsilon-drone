package amqp

import (
	log "github.com/sirupsen/logrus"
	"encoding/json"
)

func encodeMessage(in interface{}) []byte {
	jsonBytes, err := json.Marshal(in)

	if err != nil {
		log.Warnf("Could not encode message %v", err)
	}

	return jsonBytes
}
