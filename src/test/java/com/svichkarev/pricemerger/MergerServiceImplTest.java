package com.svichkarev.pricemerger;


import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class MergerServiceImplTest {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private MergerService service = new MergerServiceImpl();

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
    public void newInnerPrice() {
        final Price price1 = new Price(1, "1", 1, 1,
                time("01.01.2013 00:00:00"), time("31.01.2013 23:59:59"), 1);
        final Price price2 = new Price(1, "1", 1, 1,
                time("10.01.2013 00:00:00"), time("15.01.2013 23:59:59"), 2);

        assertThat(service.mergePrices(
                singletonList(price1),
                singletonList(price2)
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