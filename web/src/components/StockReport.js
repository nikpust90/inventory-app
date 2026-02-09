/**
 * Компонент для отображения отчета по остаткам на складах
 * Показывает иерархию: Склад -> Номенклатура -> Серии
 */
import React, { useState, useEffect } from 'react';
import { getStockReport } from '../services/stockApi';
import './StockReport.css';

const TYPE_WAREHOUSE = 0;
const TYPE_NOMENCLATURE = 1;
const TYPE_SERIES = 2;

/**
 * Компонент StockReport - отображение остатков на складах
 * @param {Array<string>} warehouseIds - Список ID складов для загрузки
 */
const StockReport = ({ warehouseIds = [] }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [stockData, setStockData] = useState([]);
  const [originalStockData, setOriginalStockData] = useState([]); // Сохраняем оригинальные данные
  const [expandedItems, setExpandedItems] = useState(new Set());
  const [warehouseSearch, setWarehouseSearch] = useState('');
  const [nomenclatureSearch, setNomenclatureSearch] = useState('');

  useEffect(() => {
    if (warehouseIds.length > 0) {
      loadStockData();
    }
  }, [warehouseIds]);

  /**
   * Фильтрация данных по поисковым запросам
   */
  useEffect(() => {
    if (originalStockData.length === 0) {
      return;
    }

    const warehouseFilter = warehouseSearch.trim().toLowerCase();
    const nomenclatureFilter = nomenclatureSearch.trim().toLowerCase();

    // Если оба фильтра пусты, показываем все данные
    if (!warehouseFilter && !nomenclatureFilter) {
      setStockData(originalStockData);
      return;
    }

    // Фильтруем данные
    const filtered = originalStockData
      .map(warehouse => {
        // Проверяем, подходит ли склад по фильтру склада
        const warehouseMatches = !warehouseFilter || 
          warehouse.name.toLowerCase().includes(warehouseFilter) ||
          warehouse.id.toLowerCase().includes(warehouseFilter);

        if (!warehouseMatches) {
          return null;
        }

        // Фильтруем номенклатуры
        const filteredItems = warehouse.children
          .map(nomenclature => {
            // Проверяем, подходит ли номенклатура по фильтру номенклатуры
            const nomenclatureMatches = !nomenclatureFilter ||
              nomenclature.name.toLowerCase().includes(nomenclatureFilter) ||
              nomenclature.id.toLowerCase().includes(nomenclatureFilter);

            if (!nomenclatureMatches) {
              return null;
            }

            return nomenclature;
          })
          .filter(item => item !== null);

        // Если нет подходящих номенклатур, но есть фильтр по номенклатуре - склад не показываем
        if (nomenclatureFilter && filteredItems.length === 0) {
          return null;
        }

        // Создаем копию склада с отфильтрованными номенклатурами
        return {
          ...warehouse,
          children: filteredItems
        };
      })
      .filter(warehouse => warehouse !== null);

    setStockData(filtered);

    // Автоматически раскрываем только склады (номенклатуры и серии не раскрываем)
    const newExpanded = new Set();
    filtered.forEach(warehouse => {
      newExpanded.add(warehouse.id);
      // Номенклатуры не раскрываем автоматически, чтобы серии не были видны
    });
    setExpandedItems(newExpanded);
  }, [warehouseSearch, nomenclatureSearch, originalStockData]);

  /**
   * Загрузка данных по остаткам
   */
  const loadStockData = async () => {
    setLoading(true);
    setError(null);

    try {
      const warehouseIdsParam = warehouseIds.join(',');
      const reports = await getStockReport(warehouseIdsParam);
      
      // Преобразуем данные в плоский список для отображения
      const flatList = processStockData(reports);
      setOriginalStockData(flatList); // Сохраняем оригинальные данные
      setStockData(flatList);
    } catch (err) {
      setError('Ошибка загрузки данных. Проверьте подключение к интернету.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Обработка данных для отображения
   */
  const processStockData = (reports) => {
    const flatList = [];

    for (const report of reports) {
      // Создаем элемент склада
      const warehouseItem = {
        type: TYPE_WAREHOUSE,
        id: report.warehouseId,
        name: report.warehouseName,
        children: []
      };

      // Добавляем номенклатуры
      if (report.items) {
        for (const item of report.items) {
          const nomenclatureItem = {
            type: TYPE_NOMENCLATURE,
            id: item.nomenclatureId,
            name: item.nomenclatureName,
            totalQuantity: item.totalQuantity,
            reserveQuantity: item.reserveQuantity,
            freeQuantity: item.freeQuantity,
            inTransitQuantity: item.inTransitQuantity,
            children: []
          };

          // Добавляем серии
          if (item.series) {
            for (const seriya of item.series) {
              const seriesItem = {
                type: TYPE_SERIES,
                id: seriya.id || seriya.seriyaId,
                name: seriya.name || seriya.seriya,
                imei: seriya.imei
              };
              nomenclatureItem.children.push(seriesItem);
            }
          }

          warehouseItem.children.push(nomenclatureItem);
        }
      }

      flatList.push(warehouseItem);
    }

    return flatList;
  };

  /**
   * Переключение раскрытия элемента
   */
  const toggleExpand = (itemId) => {
    const newExpanded = new Set(expandedItems);
    if (newExpanded.has(itemId)) {
      newExpanded.delete(itemId);
    } else {
      newExpanded.add(itemId);
    }
    setExpandedItems(newExpanded);
  };

  /**
   * Рендеринг элемента склада
   */
  const renderWarehouse = (warehouse) => {
    const isExpanded = expandedItems.has(warehouse.id);
    const hasChildren = warehouse.children && warehouse.children.length > 0;

    return (
      <div key={warehouse.id} className="stock-item warehouse">
        <div 
          className="stock-item-header"
          onClick={() => hasChildren && toggleExpand(warehouse.id)}
        >
          <span className="stock-item-icon">
            {hasChildren && (isExpanded ? '▼' : '▶')}
          </span>
          <span className="stock-item-name">
            {highlightText(warehouse.name, warehouseSearch)}
          </span>
          <span className="stock-item-id">ID: {warehouse.id}</span>
        </div>

        {isExpanded && hasChildren && (
          <div className="stock-item-children">
            {warehouse.children.map(nomenclature => renderNomenclature(nomenclature, warehouse.id))}
          </div>
        )}
      </div>
    );
  };

  /**
   * Рендеринг номенклатуры
   */
  const renderNomenclature = (nomenclature, warehouseId) => {
    const itemId = `${warehouseId}-${nomenclature.id}`;
    const isExpanded = expandedItems.has(itemId);
    const hasChildren = nomenclature.children && nomenclature.children.length > 0;

    return (
      <div key={itemId} className="stock-item nomenclature">
        <div 
          className="stock-item-header"
          onClick={() => hasChildren && toggleExpand(itemId)}
        >
          <span className="stock-item-icon">
            {hasChildren && (isExpanded ? '▼' : '▶')}
          </span>
          <span className="stock-item-name">
            {highlightText(nomenclature.name, nomenclatureSearch)}
          </span>
          <div className="stock-item-quantities">
            <span className="quantity total">Всего: {nomenclature.totalQuantity}</span>
            <span className="quantity reserve">Рез: {nomenclature.reserveQuantity}</span>
            <span className="quantity free">Своб: {nomenclature.freeQuantity}</span>
            {nomenclature.inTransitQuantity > 0 && (
              <span className="quantity transit">В пути: {nomenclature.inTransitQuantity}</span>
            )}
          </div>
        </div>

        {isExpanded && hasChildren && (
          <div className="stock-item-children">
            {nomenclature.children.map(series => renderSeries(series, itemId))}
          </div>
        )}
      </div>
    );
  };

  /**
   * Рендеринг серии
   */
  const renderSeries = (series, nomenclatureId) => {
    const itemId = `${nomenclatureId}-${series.id}`;

    return (
      <div key={itemId} className="stock-item series">
        <div className="stock-item-header">
          <span className="stock-item-icon"></span>
          <span className="stock-item-name">{series.name}</span>
          {series.imei && (
            <span className="stock-item-imei">IMEI: {series.imei}</span>
          )}
        </div>
      </div>
    );
  };

  /**
   * Подсветка найденного текста
   */
  const highlightText = (text, searchQuery) => {
    if (!searchQuery || !text) {
      return text;
    }

    const lowerText = text.toLowerCase();
    const lowerQuery = searchQuery.toLowerCase();
    const index = lowerText.indexOf(lowerQuery);

    if (index === -1) {
      return text;
    }

    const before = text.substring(0, index);
    const match = text.substring(index, index + searchQuery.length);
    const after = text.substring(index + searchQuery.length);

    return (
      <>
        {before}
        <span className="highlight">{match}</span>
        {after}
      </>
    );
  };

  return (
    <div className="stock-report-container">
      <div className="stock-report-header">
        <h1>Отчет по остаткам</h1>
        <button 
          className="refresh-button"
          onClick={loadStockData}
          disabled={loading}
        >
          {loading ? 'Загрузка...' : 'Обновить'}
        </button>
      </div>

      {/* Поля поиска */}
      <div className="search-filters">
        <div className="search-input-group">
          <label htmlFor="warehouse-search">Поиск по складу:</label>
          <input
            id="warehouse-search"
            type="text"
            className="search-input"
            placeholder="Введите название склада..."
            value={warehouseSearch}
            onChange={(e) => setWarehouseSearch(e.target.value)}
          />
          {warehouseSearch && (
            <button
              className="clear-search-button"
              onClick={() => setWarehouseSearch('')}
              title="Очистить"
            >
              ×
            </button>
          )}
        </div>

        <div className="search-input-group">
          <label htmlFor="nomenclature-search">Поиск по номенклатуре:</label>
          <input
            id="nomenclature-search"
            type="text"
            className="search-input"
            placeholder="Введите название номенклатуры..."
            value={nomenclatureSearch}
            onChange={(e) => setNomenclatureSearch(e.target.value)}
          />
          {nomenclatureSearch && (
            <button
              className="clear-search-button"
              onClick={() => setNomenclatureSearch('')}
              title="Очистить"
            >
              ×
            </button>
          )}
        </div>
      </div>

      {loading && (
        <div className="loading">
          <div className="spinner"></div>
          <p>Загрузка данных...</p>
        </div>
      )}

      {error && (
        <div className="error">
          <p>{error}</p>
          <button onClick={loadStockData}>Повторить</button>
        </div>
      )}

      {!loading && !error && stockData.length === 0 && (
        <div className="empty-state">
          <p>Нет данных по остаткам</p>
        </div>
      )}

      {!loading && !error && stockData.length > 0 && (
        <div className="stock-report-list">
          {stockData.map(warehouse => renderWarehouse(warehouse))}
        </div>
      )}
    </div>
  );
};

export default StockReport;
