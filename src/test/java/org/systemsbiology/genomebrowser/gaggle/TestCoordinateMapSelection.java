package org.systemsbiology.genomebrowser.gaggle;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class TestCoordinateMapSelection {

    @Test
    public void naturalOrderShouldBeDescending() {
        CoordinateMapSelection a = new CoordinateMapSelection("a", 0.1);
        CoordinateMapSelection b = new CoordinateMapSelection("b", 0.01);
        List<CoordinateMapSelection> list = Arrays.asList(b, a);
        Collections.sort(list);
        assertThat(list.get(0), is(a));
    }
}
