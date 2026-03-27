package ru.wert.tubus.client.entity.serviceREST;

import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import ru.wert.tubus.client.entity.api_interfaces.DecimalApiInterface;
import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.client.entity.service_interfaces.IDecimalService;
import ru.wert.tubus.client.interfaces.ItemService;
import ru.wert.tubus.client.retrofit.RetrofitClient;
import ru.wert.tubus.client.utils.BLlinks;

import java.io.IOException;
import java.util.List;

/**
 * Реализация сервиса для работы с десятичными классификаторами (Decimal)
 * Реализует паттерн Singleton для единой точки доступа
 */
@Slf4j
public class DecimalService implements IDecimalService, ItemService<Decimal> {

    private static DecimalService instance;
    private DecimalApiInterface api;

    /**
     * Приватный конструктор для реализации паттерна Singleton
     * Инициализирует API интерфейс для работы с сервером
     */
    private DecimalService() {
        BLlinks.decimalService = this;
        api = RetrofitClient.getInstance().getRetrofit().create(DecimalApiInterface.class);
    }

    /**
     * Получить API интерфейс
     * @return DecimalApiInterface
     */
    public DecimalApiInterface getApi() {
        return api;
    }

    /**
     * Получить экземпляр сервиса (Singleton)
     * @return экземпляр DecimalService
     */
    public static DecimalService getInstance() {
        if (instance == null)
            instance = new DecimalService();
        return instance;
    }

    /**
     * Найти десятичный классификатор по ID
     * @param id идентификатор классификатора
     * @return найденный Decimal или null
     */
    @Override
    public Decimal findById(Long id) {
        try {
            Call<Decimal> call = api.getById(id);
            return call.execute().body();
        } catch (IOException e) {
            log.error("Ошибка при поиске Decimal по ID: {}", id, e);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Найти десятичный классификатор по имени
     * @param name имя классификатора
     * @return найденный Decimal или null
     */
    @Override
    public Decimal findByName(String name) {
        try {
            Call<Decimal> call = api.getByName(name);
            return call.execute().body();
        } catch (IOException e) {
            log.error("Ошибка при поиске Decimal по имени: {}", name, e);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Получить все десятичные классификаторы
     * @return список всех Decimal
     */
    @Override
    public List<Decimal> findAll() {
        try {
            Call<List<Decimal>> call = api.getAll();
            return call.execute().body();
        } catch (IOException e) {
            log.error("Ошибка при получении всех Decimal", e);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Получить все десятичные классификаторы, содержащие текст в имени
     * @param text текст для поиска
     * @return список найденных Decimal
     */
    @Override
    public List<Decimal> findAllByText(String text) {
        try {
            Call<List<Decimal>> call = api.getAllByText(text);
            return call.execute().body();
        } catch (IOException e) {
            log.error("Ошибка при поиске Decimal по тексту: {}", text, e);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Сохранить (создать) новый десятичный классификатор
     * @param entity объект Decimal для сохранения
     * @return сохраненный Decimal с присвоенным ID
     */
    @Override
    public Decimal save(Decimal entity) {
        try {
            Call<Decimal> call = api.create(entity);
            return call.execute().body();
        } catch (IOException e) {
            log.error("Ошибка при сохранении Decimal: {}", entity, e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Обновить существующий десятичный классификатор
     * @param entity объект Decimal с обновленными данными
     * @return true если обновление успешно, false в противном случае
     */
    @Override
    public boolean update(Decimal entity) {
        try {
            Call<Void> call = api.update(entity);
            return call.execute().isSuccessful();
        } catch (IOException e) {
            log.error("Ошибка при обновлении Decimal: {}", entity, e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Удалить десятичный классификатор
     * @param entity удаляемый объект Decimal
     * @return true если удаление успешно, false в противном случае
     */
    @Override
    public boolean delete(Decimal entity) {
        Long id = entity.getId();
        try {
            Call<Void> call = api.deleteById(id);
            return call.execute().isSuccessful();
        } catch (IOException e) {
            log.error("Ошибка при удалении Decimal с ID: {}", id, e);
            e.printStackTrace();
            return false;
        }
    }
}
