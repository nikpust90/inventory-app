/**
 * Основной компонент для работы со схемами автомобилей
 * Реализует пошаговый выбор: категория -> марка -> модель -> поколение
 * Поддерживает поиск и отображение результатов
 */
import React, { useState, useEffect } from 'react';
import { getCategories, getBrands, getModels, getGenerations, searchCars } from '../services/carApi';
import BitrixDialog from './BitrixDialog';
import './CarScheme.css';

const STEPS = {
  CATEGORY: 0,
  BRAND: 1,
  MODEL: 2,
  GENERATION: 3,
  RESULT: 4
};

/**
 * Компонент CarScheme - основной интерфейс для работы со схемами
 */
const CarScheme = () => {
  const [currentStep, setCurrentStep] = useState(STEPS.CATEGORY);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  // Состояние выбора
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [selectedBrand, setSelectedBrand] = useState(null);
  const [selectedModel, setSelectedModel] = useState(null);
  
  // Данные для отображения
  const [items, setItems] = useState([]);
  const [generations, setGenerations] = useState([]);
  const [selectedGeneration, setSelectedGeneration] = useState(null);
  
  // Поиск
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearchMode, setIsSearchMode] = useState(false);
  
  // Bitrix диалог
  const [isBitrixDialogOpen, setIsBitrixDialogOpen] = useState(false);

  /**
   * Загрузка категорий при монтировании компонента
   */
  useEffect(() => {
    loadCategories();
  }, []);

  /**
   * Загрузка категорий автомобилей
   */
  const loadCategories = async () => {
    setLoading(true);
    setError(null);
    setIsSearchMode(false);
    setCurrentStep(STEPS.CATEGORY);
    setSelectedCategory(null);
    setSelectedBrand(null);
    setSelectedModel(null);
    
    try {
      const categories = await getCategories();
      setItems(categories);
    } catch (err) {
      setError('Ошибка загрузки категорий');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Загрузка марок по выбранной категории
   */
  const loadBrands = async (category) => {
    setLoading(true);
    setError(null);
    setSelectedCategory(category);
    setCurrentStep(STEPS.BRAND);
    
    try {
      const brands = await getBrands(category);
      setItems(brands);
    } catch (err) {
      setError('Ошибка загрузки марок');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Загрузка моделей по выбранной категории и марке
   */
  const loadModels = async (brand) => {
    setLoading(true);
    setError(null);
    setSelectedBrand(brand);
    setCurrentStep(STEPS.MODEL);
    
    try {
      const models = await getModels(selectedCategory, brand);
      setItems(models);
    } catch (err) {
      setError('Ошибка загрузки моделей');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Загрузка поколений по выбранной категории, марке и модели
   * @param {string} model - Модель автомобиля
   * @param {string} category - Категория (опционально, если не указана, используется selectedCategory)
   * @param {string} brand - Марка (опционально, если не указана, используется selectedBrand)
   */
  const loadGenerations = async (model, category = null, brand = null) => {
    setLoading(true);
    setError(null);
    setSelectedModel(model);
    setCurrentStep(STEPS.GENERATION);
    
    // Используем переданные параметры или текущее состояние
    const categoryToUse = category || selectedCategory;
    const brandToUse = brand || selectedBrand;
    
    if (!categoryToUse || !brandToUse) {
      setError('Не указаны категория или марка');
      setLoading(false);
      return;
    }
    
    try {
      const gens = await getGenerations(categoryToUse, brandToUse, model);
      setGenerations(gens);
      setItems(gens.map(g => g.name));
    } catch (err) {
      setError('Ошибка загрузки поколений');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Обработка выбора элемента на текущем шаге
   */
  const handleItemClick = (item) => {
    if (currentStep === STEPS.CATEGORY) {
      loadBrands(item);
    } else if (currentStep === STEPS.BRAND) {
      loadModels(item);
    } else if (currentStep === STEPS.MODEL) {
      loadGenerations(item);
    } else if (currentStep === STEPS.GENERATION) {
      const generation = generations.find(g => g.name === item);
      if (generation) {
        setSelectedGeneration(generation);
        setCurrentStep(STEPS.RESULT);
      }
    }
  };

  /**
   * Выполнение поиска автомобилей
   */
  const performSearch = async () => {
    const query = searchQuery.trim();
    if (query.length < 2) {
      setError('Введите минимум 2 буквы');
      return;
    }

    setLoading(true);
    setError(null);
    setIsSearchMode(true);
    
    try {
      const results = await searchCars(query);
      if (results.length === 0) {
        setError('Ничего не найдено');
        setItems([]);
      } else {
        setItems(results.map(r => r.display_name));
        // Сохраняем результаты для последующего использования
        window.searchResults = results;
      }
    } catch (err) {
      setError('Ошибка поиска');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Применение результата поиска
   */
  const applySearchResult = (displayName) => {
    const results = window.searchResults || [];
    const result = results.find(r => r.display_name === displayName);
    if (result) {
      // Устанавливаем состояние
      setSelectedCategory(result.category);
      setSelectedBrand(result.brand);
      setSelectedModel(result.model);
      setSearchQuery('');
      setIsSearchMode(false);
      
      // Загружаем поколения напрямую, передавая все параметры явно
      // Это важно, так как setState асинхронный
      loadGenerations(result.model, result.category, result.brand);
    }
  };

  /**
   * Обработка нажатия кнопки "Назад"
   */
  const handleBack = () => {
    if (currentStep === STEPS.CATEGORY) {
      return;
    } else if (currentStep === STEPS.BRAND) {
      loadCategories();
    } else if (currentStep === STEPS.MODEL) {
      loadBrands(selectedCategory);
    } else if (currentStep === STEPS.GENERATION) {
      loadModels(selectedBrand);
    } else if (currentStep === STEPS.RESULT) {
      setCurrentStep(STEPS.GENERATION);
      setSelectedGeneration(null);
    }
  };

  /**
   * Открытие ссылки в новом окне
   */
  const openLink = (url) => {
    if (url && (url.startsWith('http://') || url.startsWith('https://'))) {
      window.open(url, '_blank');
    }
  };

  /**
   * Открытие сайта Starline
   */
  const openStarline = () => {
    window.open('https://install.starline.ru', '_blank');
  };

  /**
   * Формирование хлебных крошек
   */
  const getBreadcrumbs = () => {
    const parts = [];
    if (selectedCategory) parts.push(selectedCategory);
    if (selectedBrand) parts.push(selectedBrand);
    if (selectedModel) parts.push(selectedModel);
    return parts.join(' > ');
  };

  /**
   * Получение заголовка текущего шага
   */
  const getStepTitle = () => {
    if (isSearchMode && currentStep !== STEPS.RESULT) {
      return `Результаты поиска: ${searchQuery}`;
    }
    switch (currentStep) {
      case STEPS.CATEGORY:
        return 'Выберите раздел';
      case STEPS.BRAND:
        return 'Выберите марку';
      case STEPS.MODEL:
        return 'Выберите модель';
      case STEPS.GENERATION:
        return 'Выберите поколение';
      case STEPS.RESULT:
        return `${selectedBrand} ${selectedModel} ${selectedGeneration?.name || ''}`;
      default:
        return '';
    }
  };

  return (
    <div className="car-scheme-container">
      <div className="car-scheme-header">
        <h1>Схемы автомобилей</h1>
        
        {/* Поиск */}
        <div className="search-container">
          <input
            type="text"
            className="search-input"
            placeholder="Поиск автомобиля..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && performSearch()}
          />
          <button className="search-button" onClick={performSearch}>
            Найти
          </button>
        </div>

        {/* Хлебные крошки */}
        {!isSearchMode && (
          <div className="breadcrumbs">
            {getBreadcrumbs()}
          </div>
        )}

        {/* Заголовок шага */}
        <div className="step-title">{getStepTitle()}</div>
      </div>

      {/* Кнопки дополнительных действий */}
      <div className="extra-buttons">
        <button className="extra-button" onClick={openStarline}>
          Starline
        </button>
      </div>

      {/* Контент */}
      <div className="car-scheme-content">
        {loading && (
          <div className="loading">
            <div className="spinner"></div>
            <p>Загрузка...</p>
          </div>
        )}

        {error && (
          <div className="error">
            <p>{error}</p>
            <button onClick={loadCategories}>Вернуться к началу</button>
          </div>
        )}

        {!loading && !error && currentStep !== STEPS.RESULT && (
          <div className="items-grid">
            {items.map((item, index) => (
              <button
                key={index}
                className="item-button"
                onClick={() => {
                  if (isSearchMode) {
                    applySearchResult(item);
                  } else {
                    handleItemClick(item);
                  }
                }}
              >
                {item}
              </button>
            ))}
          </div>
        )}

        {!loading && !error && currentStep === STEPS.RESULT && selectedGeneration && (
          <div className="result-container">
            <div className="result-info">
              <h2>{selectedBrand} {selectedModel} {selectedGeneration.name}</h2>
              {selectedGeneration.info && selectedGeneration.info !== 'None' && (
                <p className="result-info-text">Инфо: {selectedGeneration.info}</p>
              )}
              {!selectedGeneration.info || selectedGeneration.info === 'None' ? (
                <p className="result-info-text">Дополнительной информации нет</p>
              ) : null}
            </div>

            <div className="result-actions">
              {selectedGeneration.link && 
               (selectedGeneration.link.startsWith('http://') || 
                selectedGeneration.link.startsWith('https://')) && (
                <button
                  className="action-button primary"
                  onClick={() => openLink(selectedGeneration.link)}
                >
                  Открыть схему
                </button>
              )}
              <button
                className="action-button bitrix"
                onClick={() => setIsBitrixDialogOpen(true)}
              >
                Отправить в Битрикс
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Кнопка "Назад" */}
      {currentStep !== STEPS.CATEGORY && (
        <div className="back-button-container">
          <button className="back-button" onClick={handleBack}>
            Назад
          </button>
        </div>
      )}

      {/* Bitrix диалог */}
      <BitrixDialog
        isOpen={isBitrixDialogOpen}
        onClose={() => setIsBitrixDialogOpen(false)}
        messageText={selectedGeneration ? buildSchemeMessage(selectedGeneration, selectedCategory, selectedBrand, selectedModel) : ''}
      />
    </div>
  );

  /**
   * Формирование текста сообщения для Bitrix
   */
  function buildSchemeMessage(item, category, brand, model) {
    const sb = [];

    // Строка 1
    sb.push('Запрос схемы из базы CAN-LOG выполнен.\n');

    // Строка 2: CAN-(категория)
    if (category) {
      sb.push(`CAN-${category}\n`);
    }

    // Строка 3: Объект
    let fullDescription = '';
    if (brand) fullDescription += brand;
    if (model) fullDescription += ' ' + model;
    if (item.name) fullDescription += ' ' + item.name;
    if (item.info && item.info !== 'None' && item.info.trim()) {
      fullDescription += ' ' + item.info;
    }
    sb.push(`Объект: ${fullDescription}\n`);

    // Строка 4: Статус
    sb.push('Статус: Успешно\n\n');

    // Строка 5: Ссылка
    if (item.link && item.link.trim()) {
      sb.push('Прикрепленные ресурсы:\n');
      sb.push(`• Схема: ${item.link}`);
    } else {
      sb.push('Прикрепленные ресурсы: отсутствуют');
    }

    return sb.join('');
  }
};

export default CarScheme;
