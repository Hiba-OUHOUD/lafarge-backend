package com.lafarge.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "passing_score")
    private int passingScore = 70; // 70% par défaut

    @OneToOne
    @JoinColumn(name = "training_id", unique = true, nullable = false)
    private Training training;

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getPassingScore() { return passingScore; }
    public void setPassingScore(int passingScore) { this.passingScore = passingScore; }
    public Training getTraining() { return training; }
    public void setTraining(Training training) { this.training = training; }
}