package com.svichkarev.pricemerger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MergerServiceImpl implements MergerService {

    /**
     * {@inheritDoc}
     */
    public List<Price> mergePrices(List<Price> newPrices, List<Price> existingPrices) {
        final Map<PriceKey, List<Price>> newPricesMap = divideByPriceKey(newPrices);
        final Map<PriceKey, List<Price>> existingPricesMap = divideByPriceKey(existingPrices);

        final List<Price> mergedPrices = new ArrayList<>(existingPrices.size());
        mergedPrices.addAll(getUniquePrices(newPricesMap, existingPricesMap));

        for (Map.Entry<PriceKey, List<Price>> entry : newPricesMap.entrySet()) {
            if (!existingPricesMap.containsKey(entry.getKey())) {
                continue;
            }
            final List<Price> commonNewPrices = entry.getValue();
            final List<Price> commonExistingPrices = existingPricesMap.get(entry.getKey());

            mergedPrices.addAll(mergeCommonPrices(commonNewPrices, commonExistingPrices));
        }

        return mergedPrices;
    }

    /**
     * Слияние общих цен
     *
     * @param newPrices      Коллекция новых цен
     * @param existingPrices Коллекция имеющихся цен
     * @return Объединённые цены
     */
    List<Price> mergeCommonPrices(List<Price> newPrices, List<Price> existingPrices) {
        List<Point> pricePoints = getSortedPriceTimePoints(newPrices, existingPrices);
        return restoreMergedPrices(pricePoints, newPrices.get(0));
    }

    private List<Point> getSortedPriceTimePoints(List<Price> newPrices, List<Price> existingPrices) {
        List<Point> pricePoints = new ArrayList<>(existingPrices.size() * 2);
        for (Price newPrice : newPrices) {
            pricePoints.add(new Point(newPrice.getBegin(), Point.Type.BEGIN, Point.Generation.NEW, newPrice.getValue()));
            pricePoints.add(new Point(newPrice.getEnd(), Point.Type.END, Point.Generation.NEW, newPrice.getValue()));
        }
        for (Price existingPrice : existingPrices) {
            pricePoints.add(new Point(existingPrice.getBegin(), Point.Type.BEGIN, Point.Generation.EXISTING, existingPrice.getValue()));
            pricePoints.add(new Point(existingPrice.getEnd(), Point.Type.END, Point.Generation.EXISTING, existingPrice.getValue()));
        }
        pricePoints.sort(Comparator.comparing(p -> p.time));
        return pricePoints;
    }

    private List<Price> restoreMergedPrices(List<Point> pricePoints, Price priceAttributes) {
        final List<Price> mergedPrices = new ArrayList<>();
        Point begin = pricePoints.get(0);
        for (int i = 1; i < pricePoints.size(); i++) {
            final Point current = pricePoints.get(i);
            if (begin.type == Point.Type.END
                    && current.type == Point.Type.BEGIN) {
                begin = current;
                continue;
            }

            if (current.generation == Point.Generation.NEW) {
                if (current.value == begin.value && current.type == Point.Type.BEGIN) {
                    begin.generation = Point.Generation.NEW;
                } else {
                    addRestoredPrice(mergedPrices, priceAttributes, begin, current, begin.value);
                    begin = current;
                }
            } else {
                if (begin.generation == Point.Generation.NEW
                        && (begin.type == Point.Type.BEGIN || current.type != Point.Type.END)) {
                    continue;
                }

                long value = current.type == Point.Type.END ? current.value : begin.value;
                addRestoredPrice(mergedPrices, priceAttributes, begin, current, value);

                begin = current;
            }
        }
        return mergedPrices;
    }

    private void addRestoredPrice(List<Price> mergedPrices, Price priceAttributes, Point begin, Point end, long value) {
        if (end.time.equals(begin.time)) {
            return;
        }
        mergedPrices.add(new Price(
                priceAttributes.getId(), priceAttributes.getProductCode(),
                priceAttributes.getNumber(), priceAttributes.getDepart(),
                begin.time, end.time, value
        ));
    }

    private static class Point {
        enum Type {
            BEGIN, END
        }

        enum Generation {
            NEW, EXISTING
        }

        Date time;
        Type type;
        Generation generation;
        long value;

        public Point(Date time, Type type, Generation generation, long value) {
            this.time = time;
            this.type = type;
            this.generation = generation;
            this.value = value;
        }
    }

    /**
     * Получить цены, которые не конфликтуют среди новых и существующих
     *
     * @param newPricesMap      Коллекция новых цен
     * @param existingPricesMap Коллекция имеющихся цен
     * @return Коллекция неконфликтующих цен среди новых и существующих
     */
    private List<Price> getUniquePrices(Map<PriceKey, List<Price>> newPricesMap, Map<PriceKey, List<Price>> existingPricesMap) {
        final List<Price> uniquePrices = new ArrayList<>();
        for (PriceKey existingKey : existingPricesMap.keySet()) {
            if (!newPricesMap.containsKey(existingKey)) {
                uniquePrices.addAll(existingPricesMap.get(existingKey));
            }
        }
        for (PriceKey newKey : newPricesMap.keySet()) {
            if (!existingPricesMap.containsKey(newKey)) {
                uniquePrices.addAll(newPricesMap.get(newKey));
            }
        }

        return uniquePrices;
    }

    /**
     * Разделение цен по ключам на группы для объединения
     *
     * @param prices Коллекция цен
     * @return Таблица групп цен по ключам
     */
    private Map<PriceKey, List<Price>> divideByPriceKey(List<Price> prices) {
        final Map<PriceKey, List<Price>> map = new HashMap<>();
        for (Price price : prices) {
            final PriceKey key = new PriceKey(
                    price.getId(), price.getProductCode(), price.getNumber(), price.getDepart());
            if (!map.containsKey(key)) {
                map.put(key, new ArrayList<>());
            }
            map.get(key).add(price);
        }
        return map;
    }

    /**
     * Ключ цены для выделения группу цен для объединения
     */
    private class PriceKey {
        long id; // идентификатор в БД
        String productCode; // код товара
        int number; // номер цены
        int depart; // номер отдела

        public PriceKey(long id, String productCode, int number, int depart) {
            this.id = id;
            this.productCode = productCode;
            this.number = number;
            this.depart = depart;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PriceKey priceKey = (PriceKey) o;
            return id == priceKey.id &&
                    number == priceKey.number &&
                    depart == priceKey.depart &&
                    productCode.equals(priceKey.productCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, productCode, number, depart);
        }
    }
}
