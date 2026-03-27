package ru.wert.tubus.client.entity.api_interfaces;

import retrofit2.Call;
import retrofit2.http.*;
import ru.wert.tubus.client.entity.models.Decimal;

import java.util.List;

/**
 * Интерфейс для REST API запросов к серверу для работы с десятичными классификаторами (Decimal)
 */
public interface DecimalApiInterface {

    /**
     * Получить десятичный классификатор по ID
     * @param id идентификатор
     * @return Call<Decimal>
     */
    @GET("decimals/id/{id}")
    Call<Decimal> getById(@Path("id") Long id);

    /**
     * Получить десятичный классификатор по имени
     * @param name имя классификатора
     * @return Call<Decimal>
     */
    @GET("decimals/name/{name}")
    Call<Decimal> getByName(@Path("name") String name);

    /**
     * Получить все десятичные классификаторы
     * @return Call<List<Decimal>>
     */
    @GET("decimals/all")
    Call<List<Decimal>> getAll();

    /**
     * Получить все десятичные классификаторы, содержащие текст в имени
     * @param text текст для поиска
     * @return Call<List<Decimal>>
     */
    @GET("decimals/all-by-text/{text}")
    Call<List<Decimal>> getAllByText(@Path("text") String text);

    /**
     * Создать новый десятичный классификатор
     * @param entity объект Decimal для создания
     * @return Call<Decimal>
     */
    @POST("decimals/create")
    Call<Decimal> create(@Body Decimal entity);

    /**
     * Обновить существующий десятичный классификатор
     * @param entity объект Decimal с обновленными данными
     * @return Call<Void>
     */
    @PUT("decimals/update")
    Call<Void> update(@Body Decimal entity);

    /**
     * Удалить десятичный классификатор по ID
     * @param id идентификатор удаляемого классификатора
     * @return Call<Void>
     */
    @DELETE("decimals/delete/{id}")
    Call<Void> deleteById(@Path("id") Long id);
}
