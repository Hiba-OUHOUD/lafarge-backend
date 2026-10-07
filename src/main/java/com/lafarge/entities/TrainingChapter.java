package com.lafarge.entities;

import jakarta.persistence.*;
import jakarta.json.bind.annotation.JsonbTransient;

@Entity
@Table(name = "training_chapters")
public class TrainingChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "chapter_order", nullable = false)
    private int chapterOrder;

    @ManyToOne
    @JoinColumn(name = "training_id", nullable = false)
    @JsonbTransient
    private Training training;

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getChapterOrder() { return chapterOrder; }
    public void setChapterOrder(int chapterOrder) { this.chapterOrder = chapterOrder; }
    public Training getTraining() { return training; }
    public void setTraining(Training training) { this.training = training; }
}