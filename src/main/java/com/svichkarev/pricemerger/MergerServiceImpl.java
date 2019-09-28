package com.svichkarev.pricemerger;

import java.util.ArrayList;
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

        return mergedPrices;
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
