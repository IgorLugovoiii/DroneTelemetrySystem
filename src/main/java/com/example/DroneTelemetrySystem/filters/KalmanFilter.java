package com.example.DroneTelemetrySystem.filters;

public class KalmanFilter {
    private double x;  // Оцінене значення (координата)
    private double p = Math.random();  // Початкова невизначеність(Похибка оцінки)

    public KalmanFilter(double initialEstimate) {
        this.x = initialEstimate;
    }

    public double update(double measurement, double speed, double gpsAccuracy) {
        // Процесний шум (як швидко змінюються координати)
        double q = calculateProcessNoise(speed);
        // Шум вимірювань (GPS-похибка)
        double r = calculateMeasurementNoise(gpsAccuracy);

        p = p + q;  // Передбачення невизначеності
        double k = p / (p + r);  // Коефіцієнт Кальмана
        x = x + k * (measurement - x);  // Оновлення оцінки
        p = (1 - k) * p;  // Оновлення невизначеності
        return x;
    }

    //    q показує, наскільки швидко змінюються координати, якщо дрон швидко рухається збільшується q:
//    Статичний дрон (висить на місці) q = 0.00001
//    Плавний рух, повільний політ q = 0.0001
//    Швидкий політ або вітер q = 0.001
//    Різкі маневри q = 0.01
    private double calculateProcessNoise(double speed) {
        if (speed < 1) return 0.000001;
        if (speed < 5) return 0.00001;
        if (speed < 20) return 0.0001;
        if (speed < 50) return 0.001;
        return 0.01;
    }

    //    Значення r визначається точністю GPS-модуля, якщо сигнал слабкий збільшується r:
//    Точний GPS (похибка ±0.1 м) r = 0.1
//    Звичайний GPS (похибка ±3-5 м) r = 3.0 - 5.0
//    Дешевий модуль або сильні перешкоди r = 10.0
    private double calculateMeasurementNoise(double gpsAccuracy) {
        if (gpsAccuracy < 0.2) return 0.01;
        if (gpsAccuracy < 1) return 0.1;
        if (gpsAccuracy < 5) return 1.0;
        return 5.0;
    }

}

