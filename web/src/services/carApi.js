/**
 * API клиент для работы с автомобильным API
 * Предоставляет методы для получения категорий, марок, моделей, поколений и поиска
 */
import axios from 'axios';

// Используем относительный путь - nginx будет проксировать на backend
// В production используем прокси через nginx, в development можно указать полный URL
const BASE_URL = process.env.REACT_APP_API_URL || '/api';

const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
});

/**
 * Получить список категорий автомобилей
 * @returns {Promise<Array<string>>} Массив категорий
 */
export const getCategories = async () => {
  try {
    const response = await apiClient.get('/cars/categories');
    return response.data.categories || [];
  } catch (error) {
    console.error('Ошибка загрузки категорий:', error);
    throw error;
  }
};

/**
 * Получить список марок по категории
 * @param {string} category - Категория автомобиля
 * @returns {Promise<Array<string>>} Массив марок
 */
export const getBrands = async (category) => {
  try {
    const response = await apiClient.get('/cars/brands', {
      params: { category }
    });
    return response.data.brands || [];
  } catch (error) {
    console.error('Ошибка загрузки марок:', error);
    throw error;
  }
};

/**
 * Получить список моделей по категории и марке
 * @param {string} category - Категория автомобиля
 * @param {string} brand - Марка автомобиля
 * @returns {Promise<Array<string>>} Массив моделей
 */
export const getModels = async (category, brand) => {
  try {
    const response = await apiClient.get('/cars/models', {
      params: { category, brand }
    });
    return response.data.models || [];
  } catch (error) {
    console.error('Ошибка загрузки моделей:', error);
    throw error;
  }
};

/**
 * Получить список поколений по категории, марке и модели
 * @param {string} category - Категория автомобиля
 * @param {string} brand - Марка автомобиля
 * @param {string} model - Модель автомобиля
 * @returns {Promise<Array<GenerationItem>>} Массив поколений
 */
export const getGenerations = async (category, brand, model) => {
  try {
    const response = await apiClient.get('/cars/generations', {
      params: { category, brand, model }
    });
    return response.data.generations || [];
  } catch (error) {
    console.error('Ошибка загрузки поколений:', error);
    throw error;
  }
};

/**
 * Поиск автомобилей по запросу
 * @param {string} query - Поисковый запрос (минимум 2 символа)
 * @returns {Promise<Array<SearchResult>>} Массив результатов поиска
 */
export const searchCars = async (query) => {
  try {
    if (query.length < 2) {
      throw new Error('Поисковый запрос должен содержать минимум 2 символа');
    }
    const response = await apiClient.get('/cars/search', {
      params: { q: query }
    });
    return response.data.results || [];
  } catch (error) {
    console.error('Ошибка поиска:', error);
    throw error;
  }
};
