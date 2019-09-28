package com.svichkarev.pricemerger;


import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class MergerServiceImplTest {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private MergerServiceImpl service = new MergerServiceImpl();

    @Test
    public void onlyNewPrice() {
        final Price price = new Price(1, "1", 1, 1,
                time("01.01.2013 00:00:00"), time("31.01.2013 23:59:59"), 1);

        assertThat(service.mergePrices(
                singletonList(price),
                emptyList()
        )).containsExactly(
                price
        );
    }

    @Test
    public void onlyOldPrice() {
        final Price price = new Price(1, "1", 1, 1,
                time("01.01.2013 00:00:00"), time("31.01.2013 23:59:59"), 1);

        assertThat(service.mergePrices(
                emptyList(),
                singletonList(price)
        )).containsExactly(
                price
        );
    }

    @Test
    public void notIntersectingDifferentPrices() {
        final Price price1 = new Price(1, "1", 1, 1,
                time("01.01.2013 00:00:00"), time("31.01.2013 23:59:59"), 1);
        final Price price2 = new Price(2, "2", 2, 2,
                time("01.02.2013 00:00:00"), time("31.02.2013 23:59:59"), 2);

        assertThat(service.mergePrices(
                singletonList(price1),
                singletonList(price2)
        )).containsExactlyInAnyOrder(
                price1, price2
        );
    }

    @Test
    public void notIntersectingSamePrices() {
        final Price price1 = new Price(1, "1", 1, 1,
                time("01.01.2013 00:00:00"), time("31.01.2013 23:59:59"), 1);
        final Price price2 = new Price(1, "1", 1, 1,
                time("01.02.2013 00:00:00"), time("31.02.2013 23:59:59"), 2);

        assertThat(service.mergePrices(
                singletonList(price1),
                singletonList(price2)
        )).containsExactlyInAnyOrder(
                price1, price2
        );
    }

    @Test
    public void mergeCommonPrices_newInnerPrice() {
        final Price price1 = new Price(1, "1", 1, 1,
                time("01.01.2013 00:00:00"), time("31.01.2013 23:59:59"), 1);
        final Price price2 = new Price(1, "1", 1, 1,
                time("10.01.2013 00:00:00"), time("15.01.2013 23:59:59"), 2);

        assertThat(service.mergeCommonPrices(
                singletonList(price2),
                singletonList(price1)
        )).containsExactly(
                new Price(1, "1", 1, 1,
                        time("01.01.2013 00:00:00"), time("10.01.2013 00:00:00"), 1),
                price2,
                new Price(1, "1", 1, 1,
                        time("15.01.2013 23:59:59"), time("31.01.2013 23:59:59"), 1)
        );
    }

    @Test
    public void mergeCommonPrices_newBetweenExisting() {
        final Price exPrice1 = new Price(1, "1", 1, 1,
                time("01.01.2013 00:00:00"), time("20.01.2013 23:59:59"), 100);
        final Price exPrice2 = new Price(1, "1", 1, 1,
                time("20.01.2013 00:00:00"), time("31.01.2013 23:59:59"), 120);
        final Price newPrice = new Price(1, "1", 1, 1,
                time("15.01.2013 00:00:00"), time("25.01.2013 23:59:59"), 110);

        assertThat(service.mergeCommonPrices(
                singletonList(newPrice),
                asList(exPrice1, exPrice2)
        )).containsExactly(
                new Price(1, "1", 1, 1,
                        time("01.01.2013 00:00:00"), time("15.01.2013 00:00:00"), 100),
                new Price(1, "1", 1, 1,
                        time("15.01.2013 00:00:00"), time("25.01.2013 23:59:59"), 110),
                new Price(1, "1", 1, 1,
                        time("25.01.2013 23:59:59"), time("31.01.2013 23:59:59"), 120)
        );
    }

    @Test
    public void mergeCommonPrices_neighborsNewBetweenExisting() {
        final Price exPrice1 = new Price(1, "1", 1, 1,
                time("01.01.2013 00:00:00"), time("10.01.2013 23:59:59"), 80);
        final Price exPrice2 = new Price(1, "1", 1, 1,
                time("10.01.2013 23:59:59"), time("20.01.2013 23:59:59"), 87);
        final Price exPrice3 = new Price(1, "1", 1, 1,
                time("20.01.2013 23:59:59"), time("31.01.2013 23:59:59"), 90);

        final Price newPrice1 = new Price(1, "1", 1, 1,
                time("05.01.2013 00:00:00"), time("15.01.2013 23:59:59"), 80);
        final Price newPrice2 = new Price(1, "1", 1, 1,
                time("15.01.2013 23:59:59"), time("25.01.2013 23:59:59"), 85);

        assertThat(service.mergeCommonPrices(
                asList(newPrice1, newPrice2),
                asList(exPrice1, exPrice2, exPrice3)
        )).containsExactly(
                new Price(1, "1", 1, 1,
                        time("01.01.2013 00:00:00"), time("15.01.2013 23:59:59"), 80),
                newPrice2,
                new Price(1, "1", 1, 1,
                        time("25.01.2013 23:59:59"), time("31.01.2013 23:59:59"), 90)
        );
    }

    @Test
    public void mergeCommonPrices_notOverlap() {
        final Price exPrice1 = new Price(1, "1", 1, 1,
                time("01.01.2013 00:00:00"), time("10.01.2013 23:59:59"), 80);
        final Price newPrice1 = new Price(1, "1", 1, 1,
                time("15.01.2013 00:00:00"), time("16.01.2013 23:59:59"), 90);
        final Price newPrice2 = new Price(1, "1", 1, 1,
                time("16.01.2013 23:59:59"), time("18.01.2013 23:59:59"), 95);
        final Price exPrice2 = new Price(1, "1", 1, 1,
                time("20.01.2013 23:59:59"), time("31.01.2013 23:59:59"), 100);

        assertThat(service.mergeCommonPrices(
                asList(newPrice1, newPrice2),
                asList(exPrice1, exPrice2)
        )).containsExactly(
                exPrice1, newPrice1, newPrice2, exPrice2
        );
    }

    @Test
    public void mergeCommonPrices_commonTimeFor4Price() {
        final Price exPrice1 = new Price(1, "1", 1, 1,
                time("01.01.2013 00:00:00"), time("15.01.2013 23:59:59"), 80);
        final Price newPrice1 = new Price(1, "1", 1, 1,
                time("01.01.2013 00:00:00"), time("15.01.2013 23:59:59"), 90);
        final Price newPrice2 = new Price(1, "1", 1, 1,
                time("15.01.2013 23:59:59"), time("31.01.2013 23:59:59"), 95);
        final Price exPrice2 = new Price(1, "1", 1, 1,
                time("15.01.2013 23:59:59"), time("31.01.2013 23:59:59"), 100);

        assertThat(service.mergeCommonPrices(
                asList(newPrice1, newPrice2),
                asList(exPrice1, exPrice2)
        )).containsExactly(
                newPrice1, newPrice2
        );
    }

    @Test
    public void newInnerPrice() {
        final Price price1 = new Price(1, "1", 1, 1,
                time("01.01.2013 00:00:00"), time("31.01.2013 23:59:59"), 1);
        final Price price2 = new Price(1, "1", 1, 1,
                time("10.01.2013 00:00:00"), time("15.01.2013 23:59:59"), 2);

        assertThat(service.mergePrices(
                singletonList(price2),
                singletonList(price1)
        )).containsExactly(
                new Price(1, "1", 1, 1,
                        time("01.01.2013 00:00:00"), time("10.01.2013 00:00:00"), 1),
                price2,
                new Price(1, "1", 1, 1,
                        time("15.01.2013 23:59:59"), time("31.01.2013 23:59:59"), 1)
        );
    }

    private static Date time(String time) {
        final LocalDateTime dateTime = LocalDateTime.parse(time, formatter);
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}