package ro.ecoregistru.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.service.DeadlineService;

import static ro.ecoregistru.exception.ErrorMessageEnum.YEAR_OUT_OF_RANGE;

/**
 * BUG-056. {@code ?year=} n-avea margini: {@code -300000} sau {@code 300000000} ajungeau în Postgres ca date
 * imposibile și dădeau 500 pe treisprezece rute, iar {@code regenerate?year=1} refăcea 2025 de ani sub
 * lacătul firmei. O singură gardă, pentru orice rută care primește anul, în locul unei verificări pe fiecare.
 */
@Configuration
public class YearRangeGuard implements WebMvcConfigurer, HandlerInterceptor {

    public static final int MIN_YEAR = 2000;

    /**
     * Între 2000 și zece ani de acum: nimeni nu are mișcări înainte de 2000, iar sus e loc de ani goi
     * (probele e2e scriu în 2033–2034 ca să nu atingă datele demo). Ce se prinde sunt anii absurzi.
     */
    public static boolean inRange(int year) {
        return year >= MIN_YEAR && year <= DeadlineService.today().getYear() + 10;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/api/**");
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String year = request.getParameter("year");
        if (year != null) {
            try {
                if (!inRange(Integer.parseInt(year.trim()))) {
                    throw new BadRequestException(YEAR_OUT_OF_RANGE);
                }
            } catch (NumberFormatException notANumber) {
                // Legarea parametrului răspunde deja 400 pentru „abc".
            }
        }
        return true;
    }
}
