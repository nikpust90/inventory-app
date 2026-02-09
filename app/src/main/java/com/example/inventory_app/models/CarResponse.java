package com.example.inventory_app.models;

import java.util.List;

public class CarResponse {
    // Поля могут быть null, Retrofit заполнит только те, что пришли в JSON
    public List<String> categories;
    public List<String> brands;
    public List<String> models;
    public List<GenerationItem> generations;

    public List<SearchResult> results;

    public static class GenerationItem {
        public String name; // Год/Поколение
        public String info; // Доп. инфо (Колонка 4)
        public String link; // Ссылка (Колонка 5)
    }

    // Вложенный класс для результата поиска
    public static class SearchResult {
        public String category;
        public String brand;
        public String model;
        public String display_name; // То, что покажем на кнопке
    }
}