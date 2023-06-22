package personal.tutorial.springbootquerydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class UserDto {

    private String name;
    private int age;

}
