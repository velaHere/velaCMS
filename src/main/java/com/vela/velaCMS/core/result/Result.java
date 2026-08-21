package com.vela.velaCMS.core.result;

import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public sealed interface Result<T> permits Result.Success, Result.Failure {

    record Success<T>(T value) implements Result<T>{}
    record Failure<T>(String reason, FailureType failureType) implements Result<T>{}

    static <T> Result<T> success(T value){
        return new Success<>(value);
    }

    static <T> Result<T> failure(FailureType failureType){
        return new Failure<>(failureType.getDefaultMessage(), failureType);
    }

    static <T> Result<T> failure(String reason, FailureType failureType){
        return new Failure<>(reason, failureType);
    }

    default boolean isSuccess(){
        return this instanceof Success;
    }

    default boolean isFailure(){
        return this instanceof Failure;
    }

    default <U> Result<U> map(Function<T, U> mapper){
        return switch(this){
            case Success<T>(T value) -> Result.success(mapper.apply(value));
            case Failure<T>(String reason,FailureType failureType) -> Result.failure(reason, failureType);
        };
    }

    default <U> Result<U> tryMap(ThrowingFunction<T, U> mapper){
        return switch(this){
            case Success<T>(T value) -> {
                try{
                    yield Result.success(mapper.apply(value));
                }catch (Exception e){
                    yield Result.failure(e.getMessage(), FailureType.INTERNAL);
                }
            }
            case Failure<T>(String reason,FailureType failureType) -> Result.failure(reason, failureType);
        };
    }

    default <U> Result<U> flatMap(Function<T, Result<U>> mapper){
        return switch(this){
            case Success(var value) -> mapper.apply(value);
            case Failure(var reason, var failureType) -> Result.failure(reason, failureType);
        };
    }

    default <U> Result<U> tryFlatMap(ThrowingFunction<T, Result<U>> mapper){
        return switch(this){
            case Success(var value) -> {
                try{
                    yield mapper.apply(value);
                }catch(Exception e){
                    yield Result.failure(e.getMessage(), FailureType.INTERNAL);
                }
            }
            case Failure(var reason, var failureType) -> Result.failure(reason, failureType);
        };
    }

    static <T> Result<T> wrap(ThrowingSupplier<T> supplier){
        try{
            return Result.success(supplier.get());
        }catch(Exception e){
            return Result.failure(e.getMessage(), FailureType.INTERNAL);
        }
    }

    static Result<Void> wrapVoid(ThrowingRunnable runnable){
        try{
            runnable.run();
            return Result.success(null);
        }catch(Exception e){
            return Result.failure(e.getMessage(), FailureType.INTERNAL);
        }
    }

    default Result<T> peek(Consumer<T> consumer){
        if(this instanceof Success<T>(T value))
            consumer.accept(value);
        return this;
    }

    default Result<T> peekFailure(Consumer<Failure<T>> consumer){
        if(this instanceof Failure<T> f)
            consumer.accept(f);
        return this;
    }

    default Result<T> recoverFrom(FailureType type, ThrowingSupplier<T> recovery){
        return switch(this){
            case Success<T> success -> success;
            case Failure<T>(String ignored, FailureType ft) when type == ft -> wrap(recovery);
            case Failure<T> failure -> failure;
        };
    }


    default <U> U fold(
            Function<T, U> onSuccess,
            Function<Failure<T>, U> onFailure
    ){
        return switch(this){
            case Success(var value) -> onSuccess.apply(value);
            case Failure<T> f -> onFailure.apply(f);
        };
    }

    default T orElse(T fallback){
        return switch(this){
            case Success(var value) -> value;
            case Failure<T> ignored -> fallback;
        };
    }

    default T orElseGet(Function<Failure<T>, T> fallback){
        return switch (this){
            case Success<T>(T value) -> value;
            case Failure<T> f -> fallback.apply(f);
        };
    }

    default Optional<T> toOptional(){
        return switch(this){
            case Success<T>(T value) -> Optional.ofNullable(value);
            case Failure<T> ignored -> Optional.empty();
        };
    }

    default T getOrThrow(){
        return switch(this){
            case Success(var value) -> value;
            case Failure(var reason, var failureType) ->
                    throw new ResponseStatusException(failureType.getStatus(), "Failure: " + reason);
        };
    }
}
