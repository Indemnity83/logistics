package com.logistics.core.machine.area;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PlotScannerTest {

    private static void emptyPass(PlotScanner scanner) {
        for (int i = 0; i < scanner.positionCount(); i++) {
            scanner.next();
        }
    }

    @Test
    void nextVisitsEveryPositionThenWraps() {
        PlotScanner scanner = new PlotScanner(4, 10, 80);

        assertThat(scanner.next()).isEqualTo(0);
        assertThat(scanner.next()).isEqualTo(1);
        assertThat(scanner.next()).isEqualTo(2);
        assertThat(scanner.next()).isEqualTo(3);
        assertThat(scanner.cursor()).isZero();
        assertThat(scanner.next()).isEqualTo(0);
    }

    @Test
    void noBackOffUntilAFullPassIsEmpty() {
        PlotScanner scanner = new PlotScanner(4, 10, 80);

        scanner.next();
        scanner.next();
        scanner.next();
        assertThat(scanner.idleTicks()).isZero();
        scanner.next();
        assertThat(scanner.idleTicks()).isEqualTo(10);
    }

    @Test
    void backOffDoublesPerEmptyPassUpToCap() {
        PlotScanner scanner = new PlotScanner(3, 10, 50);

        emptyPass(scanner);
        assertThat(scanner.idleTicks()).isEqualTo(10);
        emptyPass(scanner);
        assertThat(scanner.idleTicks()).isEqualTo(20);
        emptyPass(scanner);
        assertThat(scanner.idleTicks()).isEqualTo(40);
        emptyPass(scanner);
        assertThat(scanner.idleTicks()).isEqualTo(50);
        emptyPass(scanner);
        assertThat(scanner.idleTicks()).isEqualTo(50);
    }

    @Test
    void workResetsBackOff() {
        PlotScanner scanner = new PlotScanner(3, 10, 80);
        emptyPass(scanner);
        emptyPass(scanner);
        assertThat(scanner.idleTicks()).isEqualTo(20);

        scanner.next();
        scanner.markWorked();
        assertThat(scanner.idleTicks()).isZero();

        scanner.next();
        scanner.next();
        assertThat(scanner.idleTicks()).as("pass with work is not empty").isZero();
        emptyPass(scanner);
        assertThat(scanner.idleTicks()).as("back-off restarts from base").isEqualTo(10);
    }

    @Test
    void workOnLastPositionOfPassCountsForThatPassOnly() {
        PlotScanner scanner = new PlotScanner(3, 10, 80);

        scanner.next();
        scanner.next();
        scanner.next();
        scanner.markWorked();
        assertThat(scanner.idleTicks()).isZero();

        emptyPass(scanner);
        assertThat(scanner.idleTicks()).isEqualTo(10);
    }

    @Test
    void singlePositionScannerBacksOffEveryEmptyCall() {
        PlotScanner scanner = new PlotScanner(1, 5, 20);

        assertThat(scanner.next()).isZero();
        assertThat(scanner.idleTicks()).isEqualTo(5);
        assertThat(scanner.next()).isZero();
        assertThat(scanner.idleTicks()).isEqualTo(10);
        scanner.markWorked();
        assertThat(scanner.idleTicks()).isZero();
    }

    @Test
    void resizeKeepsInRangeCursorAndResetsBackOff() {
        PlotScanner scanner = new PlotScanner(81, 10, 80);
        for (int i = 0; i < 81 + 40; i++) {
            scanner.next();
        }
        assertThat(scanner.cursor()).isEqualTo(40);
        assertThat(scanner.idleTicks()).isEqualTo(10);

        scanner.resize(169);
        assertThat(scanner.positionCount()).isEqualTo(169);
        assertThat(scanner.cursor()).isEqualTo(40);
        assertThat(scanner.idleTicks()).isZero();
    }

    @Test
    void resizeResetsOutOfRangeCursor() {
        PlotScanner scanner = new PlotScanner(169, 10, 80);
        for (int i = 0; i < 100; i++) {
            scanner.next();
        }

        scanner.resize(81);
        assertThat(scanner.cursor()).isZero();
        assertThat(scanner.next()).isZero();
    }

    @Test
    void resizeStartsAFreshPass() {
        PlotScanner scanner = new PlotScanner(4, 10, 80);
        scanner.next();
        scanner.next();
        scanner.next();

        scanner.resize(4);
        scanner.next();
        assertThat(scanner.idleTicks()).as("pass restarted by resize").isZero();
        scanner.next();
        scanner.next();
        scanner.next();
        assertThat(scanner.idleTicks()).isEqualTo(10);
    }

    @Test
    void restoreResumesAtSavedCursor() {
        PlotScanner scanner = PlotScanner.restore(81, 37, 10, 80);

        assertThat(scanner.positionCount()).isEqualTo(81);
        assertThat(scanner.idleTicks()).isZero();
        assertThat(scanner.next()).isEqualTo(37);
        assertThat(scanner.cursor()).isEqualTo(38);
    }

    @Test
    void restoreDropsOutOfRangeCursor() {
        assertThat(PlotScanner.restore(81, 81, 10, 80).cursor()).isZero();
        assertThat(PlotScanner.restore(81, 200, 10, 80).cursor()).isZero();
        assertThat(PlotScanner.restore(81, -1, 10, 80).cursor()).isZero();
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new PlotScanner(0, 10, 80)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlotScanner(9, 0, 80)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlotScanner(9, 20, 10)).isInstanceOf(IllegalArgumentException.class);
        PlotScanner scanner = new PlotScanner(9, 10, 80);
        assertThatThrownBy(() -> scanner.resize(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
