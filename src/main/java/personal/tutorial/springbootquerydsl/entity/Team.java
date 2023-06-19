package personal.tutorial.springbootquerydsl.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //엔티티 생성에는 기본 생성자가 필요하다..
@ToString(of = {"id", "name"})
public class Team {
    @Id @GeneratedValue @Column(name = "team_id")
    private Long id;
    private String name;

    public Team(String name) {
        this.name = name;
    }

    @OneToMany(mappedBy = "team") //Member 객체 중 Column 명 team 에 대응시킨다는 뜻이ㅏㄷ.
    private List<Member> members = new ArrayList<>();

    public void setName(String name) {
        this.name = name;
    }

    public void setMembers(List<Member> members) {
        this.members = members;
    }
}
