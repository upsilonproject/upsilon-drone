package fabricConfig

import (
	"testing"
	"github.com/stretchr/testify/assert"
)

func TestArgMapCount(t *testing.T) {
	mapping := make(map[string]ArgumentMapping)
	mapping["foo"] = ArgumentMapping {
		Values: []string {
			"bar1",
			"bar2",
		},
	}

	mapping["cake"] = ArgumentMapping {
		Values: []string {
			"one",
			"two",
			"three",
		},
	}

	ret := argumentListToMap("testing", mapping);

	assert.Equal(t, 6, len(ret), "Correct permutation count")
}
