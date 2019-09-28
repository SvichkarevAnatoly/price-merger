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
        final Price price = price("01.01.2013 00:00:00", "31.01.2013 23:59:59", 1);

        assertThat(service.mergePrices(
                singletonList(price),
                emptyList()
        )).containsExactly(
                price
        );
    }

    @Test
    public void onlyOldPrice() {
        final Price price = price("01.01.2013 00:00:00", "31.01.2013 23:59:59", 1);

        assertThat(service.mergePrices(
                emptyList(),
                singletonList(price)
        )).containsExactly(
                price
        );
    }

    @Test
    public void notIntersectingDifferentPrices() {
        final Price price1 = price("01.01.2013 00:00:00", "31.01.2013 23:59:59", 1);
        final Price price2 = price("01.02.2013 00:00:00", "31.02.2013 23:59:59", 2);

        assertThat(service.mergePrices(
                singletonList(price1),
                singletonList(price2)
        )).containsExactlyInAnyOrder(
                price1, price2
        );
    }

    @Test
    public void notIntersectingSamePrices() {
        final Price price1 = price("01.01.2013 00:00:00", "31.01.2013 23:59:59", 1);
        final Price price2 = price("01.02.2013 00:00:00", "31.02.2013 23:59:59", 2);

        assertThat(service.mergePrices(
                singletonList(price1),
                singletonList(price2)
        )).containsExactlyInAnyOrder(
                price1, price2
        );
    }

    @Test
    public void mergeCommonPrices_newInnerPrice() {
        final Price price1 = price("01.01.2013 00:00:00", "31.01.2013 23:59:59", 1);
        final Price price2 = price("10.01.2013 00:00:00", "15.01.2013 23:59:59", 2);

        assertThat(service.mergeCommonPrices(
                singletonList(price2),
                singletonList(price1)
        )).containsExactly(
                price("01.01.2013 00:00:00", "10.01.2013 00:00:00", 1),
                price2,
                price("15.01.2013 23:59:59", "31.01.2013 23:59:59", 1)
        );
    }

    @Test
    public void mergeCommonPrices_newBetweenExisting() {
        final Price exPrice1 = price("01.01.2013 00:00:00", "20.01.2013 23:59:59", 100);
        final Price exPrice2 = price("20.01.2013 00:00:00", "31.01.2013 23:59:59", 120);
        final Price newPrice = price("15.01.2013 00:00:00", "25.01.2013 23:59:59", 110);

        assertThat(service.mergeCommonPrices(
                singletonList(newPrice),
                asList(exPrice1, exPrice2)
        )).containsExactly(
                price("01.01.2013 00:00:00", "15.01.2013 00:00:00", 100),
                price("15.01.2013 00:00:00", "25.01.2013 23:59:59", 110),
                price("25.01.2013 23:59:59", "31.01.2013 23:59:59", 120)
        );
    }

    @Test
    public void mergeCommonPrices_neighborsNewBetweenExisting() {
        final Price exPrice1 = price("01.01.2013 00:00:00", "10.01.2013 23:59:59", 80);
        final Price exPrice2 = price("10.01.2013 23:59:59", "20.01.2013 23:59:59", 87);
        final Price exPrice3 = price("20.01.2013 23:59:59", "31.01.2013 23:59:59", 90);

        final Price newPrice1 = price("05.01.2013 00:00:00", "15.01.2013 23:59:59", 80);
        final Price newPrice2 = price("15.01.2013 23:59:59", "25.01.2013 23:59:59", 85);

        assertThat(service.mergeCommonPrices(
                asList(newPrice1, newPrice2),
                asList(exPrice1, exPrice2, exPrice3)
        )).containsExactly(
                price("01.01.2013 00:00:00", "15.01.2013 23:59:59", 80),
                newPrice2,
                price("25.01.2013 23:59:59", "31.01.2013 23:59:59", 90)
        );
    }

    @Test
    public void mergeCommonPrices_notOverlap() {
        final Price exPrice1 = price("01.01.2013 00:00:00", "10.01.2013 23:59:59", 80);
        final Price newPrice1 = price("15.01.2013 00:00:00", "16.01.2013 23:59:59", 90);
        final Price newPrice2 = price("16.01.2013 23:59:59", "18.01.2013 23:59:59", 95);
        final Price exPrice2 = price("20.01.2013 23:59:59", "31.01.2013 23:59:59", 100);

        assertThat(service.mergeCommonPrices(
                asList(newPrice1, newPrice2),
                asList(exPrice1, exPrice2)
        )).containsExactly(
                exPrice1, newPrice1, newPrice2, exPrice2
        );
    }

    @Test
    public void mergeCommonPrices_commonTimeFor4Price() {
        final Price exPrice1 = price("01.01.2013 00:00:00", "15.01.2013 23:59:59", 80);
        final Price newPrice1 = price("01.01.2013 00:00:00", "15.01.2013 23:59:59", 90);
        final Price newPrice2 = price("15.01.2013 23:59:59", "31.01.2013 23:59:59", 95);
        final Price exPrice2 = price("15.01.2013 23:59:59", "31.01.2013 23:59:59", 100);

        assertThat(service.mergeCommonPrices(
                asList(newPrice1, newPrice2),
                asList(exPrice1, exPrice2)
        )).containsExactly(
                newPrice1, newPrice2
        );
    }

    @Test
    public void newInnerPrice() {
        final Price price1 = price("01.01.2013 00:00:00", "31.01.2013 23:59:59", 1);
        final Price price2 = price("10.01.2013 00:00:00", "15.01.2013 23:59:59", 2);

        assertThat(service.mergePrices(
                singletonList(price2),
                singletonList(price1)
        )).containsExactly(
                price("01.01.2013 00:00:00", "10.01.2013 00:00:00", 1),
                price2,
                price("15.01.2013 23:59:59", "31.01.2013 23:59:59", 1)
        );
    }

    @Test
    public void complexExample() {
        final Price exPrice1 = new Price("122856", 1, 1, time("01.01.2013 00:00:00"), time("31.01.2013 23:59:59"), 11000);
        final Price exPrice2 = new Price("122856", 2, 1, time("10.01.2013 00:00:00"), time("20.01.2013 23:59:59"), 99000);
        final Price exPrice3 = new Price("6654", 1, 2, time("01.01.2013 00:00:00"), time("31.01.2013 00:00:00"), 5000);

        final Price newPrice1 = new Price("122856", 1, 1, time("20.01.2013 00:00:00"), time("20.02.2013 23:59:59"), 11000);
        final Price newPrice2 = new Price("122856", 2, 1, time("15.01.2013 00:00:00"), time("25.01.2013 23:59:59"), 92000);
        final Price newPrice3 = new Price("6654", 1, 2, time("12.01.2013 00:00:00"), time("13.01.2013 00:00:00"), 4000);

        final Price mergedPrice1 = new Price("122856", 1, 1, time("01.01.2013 00:00:00"), time("20.02.2013 23:59:59"), 11000);
        final Price mergedPrice2 = new Price("122856", 2, 1, time("10.01.2013 00:00:00"), time("15.01.2013 00:00:00"), 99000);
        final Price mergedPrice3 = new Price("122856", 2, 1, time("15.01.2013 00:00:00"), time("25.01.2013 23:59:59"), 92000);
        final Price mergedPrice4 = new Price("6654", 1, 2, time("01.01.2013 00:00:00"), time("12.01.2013 00:00:00"), 5000);
        final Price mergedPrice5 = new Price("6654", 1, 2, time("12.01.2013 00:00:00"), time("13.01.2013 00:00:00"), 4000);
        final Price mergedPrice6 = new Price("6654", 1, 2, time("13.01.2013 00:00:00"), time("31.01.2013 00:00:00"), 5000);

        assertThat(service.mergePrices(
                asList(newPrice1, newPrice2, newPrice3),
                asList(exPrice1, exPrice2, exPrice3)
        )).containsExactlyInAnyOrder(
                mergedPrice1, mergedPrice2, mergedPrice3,
                mergedPrice4, mergedPrice5, mergedPrice6
        );
    }

    private static Price price(String begin, String end, long value) {
        return new Price("1", 1, 1, time(begin), time(end), value);
    }

    private static Date time(String time) {
        final LocalDateTime dateTime = LocalDateTime.parse(time, formatter);
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}