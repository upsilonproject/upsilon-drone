package fabricConfig

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestArgumentsToExecutionsPermutations(t *testing.T) {
	arguments := map[string]any{
		"hostname": []interface{}{"one", "two"},
		"port":     "80",
	}

	ret := argumentsToExecutions(arguments)

	assert.Len(t, ret, 2)
	assert.Equal(t, "one", ret[0]["hostname"])
	assert.Equal(t, "two", ret[1]["hostname"])
	assert.Equal(t, "80", ret[0]["port"])
}
