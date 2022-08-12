package util;

func SliceContainsElement(search []string, element string) bool {
	for _, candidate := range search {
		if candidate == element {
			return true
		}
	}

	return false
}
