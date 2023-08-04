package com.example.mutsasns.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Where;

@Entity
@NoArgsConstructor
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(name = "comment")
@Where(clause = "deleted = false")
public class CommentEntity extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    private boolean deleted;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @ToString.Exclude
    private ArticleEntity articleEntity;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @ToString.Exclude
    private UserEntity userEntity;
}