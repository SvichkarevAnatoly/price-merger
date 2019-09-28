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

        // Сразу выбираем цены, которые не нужно сливать
        final List<Price> mergedPrices = new ArrayList<>(existingPrices.size());
        mergedPrices.addAll(getUniquePrices(newPricesMap, existingPricesMap));

        mergedPrices.addAll(getSameAttributesMergedPrices(newPricesMap, existingPricesMap));
        return mergedPrices;
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
            final PriceKey key = new PriceKey(price.getProductCode(), price.getNumber(), price.getDepart());
            if (!map.containsKey(key)) {
                map.put(key, new ArrayList<>());
            }
            map.get(key).add(price);
        }
        return map;
    }

    /**
     * Получить цены, которые не нужно объединять среди новых и существующих
     *
     * @param newPricesMap      Коллекция новых цен
     * @param existingPricesMap Коллекция имеющихся цен
     * @return Коллекция неконфликтующих цен среди новых и существующих
     */
    private List<Price> getUniquePrices(Map<PriceKey, List<Price>> newPricesMap,
                                        Map<PriceKey, List<Price>> existingPricesMap) {
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
     * Слияние цен для групп цен с одинаковыми атрибутами
     *
     * @param newPricesMap      Коллекция новых цен
     * @param existingPricesMap Коллекция имеющихся цен
     * @return Объединённые цены
     */
    private List<Price> getSameAttributesMergedPrices(Map<PriceKey, List<Price>> newPricesMap,
                                                      Map<PriceKey, List<Price>> existingPricesMap) {
        final List<Price> mergedPrices = new ArrayList<>();
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
        List<PricePoint> pricePoints = getSortedPriceTimePoints(newPrices, existingPrices);
        return restoreMergedPrices(pricePoints, newPrices.get(0));
    }

    /**
     * Разбиение интервалов цены на точки начала и конца действия цены
     *
     * @param newPrices      Коллекция новых цен
     * @param existingPrices Коллекция имеющихся цен
     * @return Отсортированный по времени список временных точек изменения цены
     */
    private List<PricePoint> getSortedPriceTimePoints(List<Price> newPrices, List<Price> existingPrices) {
        List<PricePoint> pricePoints = new ArrayList<>(existingPrices.size() * 2);
        for (Price newPrice : newPrices) {
            pricePoints.add(new PricePoint(newPrice.getBegin(), PricePoint.Type.BEGIN, PricePoint.Generation.NEW, newPrice.getValue()));
            pricePoints.add(new PricePoint(newPrice.getEnd(), PricePoint.Type.END, PricePoint.Generation.NEW, newPrice.getValue()));
        }
        for (Price existingPrice : existingPrices) {
            pricePoints.add(new PricePoint(existingPrice.getBegin(), PricePoint.Type.BEGIN, PricePoint.Generation.EXISTING, existingPrice.getValue()));
            pricePoints.add(new PricePoint(existingPrice.getEnd(), PricePoint.Type.END, PricePoint.Generation.EXISTING, existingPrice.getValue()));
        }
        pricePoints.sort(Comparator.comparing(p -> p.time));
        return pricePoints;
    }

    /**
     * Восстановление цен из временных точек для получения объединённой коллекции цен
     *
     * @param pricePoints     Отсортированный по времени список временных точек изменения цены
     * @param priceAttributes Общие атрибуты цен
     * @return Объединённая коллекция цен
     */
    private List<Price> restoreMergedPrices(List<PricePoint> pricePoints, Price priceAttributes) {
        final List<Price> mergedPrices = new ArrayList<>();
        PricePoint begin = pricePoints.get(0);
        for (int i = 1; i < pricePoints.size(); i++) {
            final PricePoint current = pricePoints.get(i);
            // Если начальная точка - это конец цены, а конечная - начало,
            // тогда это временной промежуток между ценами - пропускаем
            if (begin.type == PricePoint.Type.END
                    && current.type == PricePoint.Type.BEGIN) {
                begin = current;
                continue;
            }

            // Новая ценовая точка с большим приоритетом
            if (current.generation == PricePoint.Generation.NEW) {
                if (current.value == begin.value && current.type == PricePoint.Type.BEGIN) {
                    // Если значения одинаковы, но точка из новых, поднимаем приоритет
                    begin.generation = PricePoint.Generation.NEW;
                } else {
                    // Старая цена прерывается новой - добавляем интервал
                    addRestoredPrice(mergedPrices, priceAttributes, begin, current, begin.value);
                    begin = current;
                }
            } else {
                // Начало интервала и так новая цена - перекрывает старую цену - пропускаем
                if (begin.generation == PricePoint.Generation.NEW
                        && begin.type == PricePoint.Type.BEGIN) {
                    continue;
                }

                // Цена определяется текущей точкой - окончание интервала или нет
                long value = current.type == PricePoint.Type.END ? current.value : begin.value;
                addRestoredPrice(mergedPrices, priceAttributes, begin, current, value);

                begin = current;
            }
        }
        return mergedPrices;
    }

    /**
     * Добавление восстановленного интервала действия цены
     *
     * @param mergedPrices    Коллекция цен после слияния
     * @param priceAttributes Общие атрибуты цен
     * @param begin           Время начала действия цены
     * @param end             Время конца действия цены
     * @param value           Значение цены
     */
    private void addRestoredPrice(List<Price> mergedPrices, Price priceAttributes, PricePoint begin, PricePoint end, long value) {
        // Если интервал 0, не добавляем
        if (end.time.equals(begin.time)) {
            return;
        }
        mergedPrices.add(new Price(priceAttributes.getProductCode(),
                priceAttributes.getNumber(), priceAttributes.getDepart(),
                begin.time, end.time, value
        ));
    }

    /**
     * Модель временной точки изменения цены
     */
    private static class PricePoint {
        /**
         * Тип точки - начало действия или конец
         */
        enum Type {
            BEGIN, END
        }

        /**
         * Новые цены или старые
         */
        enum Generation {
            NEW, EXISTING
        }

        private Date time;
        private Type type;
        private Generation generation;
        private long value;

        PricePoint(Date time, Type type, Generation generation, long value) {
            this.time = time;
            this.type = type;
            this.generation = generation;
            this.value = value;
        }
    }

    /**
     * Ключ цены для выделения группу цен для объединения
     */
    private class PriceKey {
        String productCode; // код товара
        int number; // номер цены
        int depart; // номер отдела

        public PriceKey(String productCode, int number, int depart) {
            this.productCode = productCode;
            this.number = number;
            this.depart = depart;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PriceKey priceKey = (PriceKey) o;
            return number == priceKey.number &&
                    depart == priceKey.depart &&
                    productCode.equals(priceKey.productCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productCode, number, depart);
        }
    }
}
