package hello.core;// <-이 설정정보로 시작

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(//default는 hello.core안에 있는 패키지를 다 뒤짐
//        basePackages = "hello.core.member", //여기 위치부터 스캔해라. (member패키지 내의 component annotation만 등록됨) //{"",""}두개도 지정가능
//        basePackageClasses = AutoAppConfig.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION , classes= Configuration.class)
)
public class AutoAppConfig {

}
