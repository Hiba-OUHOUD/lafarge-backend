package com.lafarge.services;

import com.lafarge.entities.Quiz;
import com.lafarge.entities.Question;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class QuizService {

    @PersistenceContext(unitName = "LafargePU")
    private EntityManager em;

    // Créer un quiz
    public void createQuiz(Quiz quiz) {
        em.persist(quiz);
    }

    // Récupérer un quiz par formation ID
    public Quiz findByTrainingId(Long trainingId) {
        try {
            return em.createQuery("SELECT q FROM Quiz q WHERE q.training.id = :trainingId", Quiz.class)
                     .setParameter("trainingId", trainingId)
                     .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    // Ajouter une question
    public void addQuestion(Question question) {
        em.persist(question);
    }

    // Récupérer les questions d'un quiz
    public List<Question> getQuestionsByQuizId(Long quizId) {
        return em.createQuery("SELECT q FROM Question q WHERE q.quiz.id = :quizId", Question.class)
                 .setParameter("quizId", quizId)
                 .getResultList();
    }
}
