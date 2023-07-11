package personal.tutorial.springbootquerydsl.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //엔티티 생성에는 기본 생성자가 필요하다..
    @ToString(of = {"id", "username", "age"})
public class Member {
    @Id @GeneratedValue @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY) //XtoOne에서는 항상 fetch를 적어주도록 한다.
    @JoinColumn(name = "team_id") //외래 키 아이디를 말함(Team에서의 Column 중 "team_id" 컬럼과 연계한다는 뜻)
    private Team team;

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }

    //임시다와
    public Member(String username, int age) {
        this.username = username;
        this.age = age;
        this.team = null;
    }

    public Member(String username) {
        this.username = username;
        this.age = 0;
    }

    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this); //양쪽 연관됐으니, 이것도 변경해주어야 쌔
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
}
