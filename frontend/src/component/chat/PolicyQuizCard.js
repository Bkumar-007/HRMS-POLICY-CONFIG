"use client";

import { useMemo, useState } from "react";
import Button from "@component/common/Button";

export default function PolicyQuizCard({ questions = [] }) {
  const [index, setIndex] = useState(0);
  const [selected, setSelected] = useState({});
  const [revealed, setRevealed] = useState({});

  const current = questions[index];
  const score = useMemo(() => {
    return questions.reduce((total, question) => {
      if (selected[question.id] === question.correctIndex) {
        return total + 1;
      }
      return total;
    }, 0);
  }, [questions, selected]);

  if (!questions.length || !current) {
    return null;
  }

  const choice = selected[current.id];
  const showAnswer = revealed[current.id];
  const finished = Object.keys(revealed).length === questions.length;

  function pickOption(optionIndex) {
    if (revealed[current.id]) {
      return;
    }
    setSelected((prev) => ({ ...prev, [current.id]: optionIndex }));
    setRevealed((prev) => ({ ...prev, [current.id]: true }));
  }

  return (
    <div className="quiz-card">
      <div className="quiz-card-header">
        <strong>Policy training quiz</strong>
        <span className="muted">
          Question {index + 1} / {questions.length}
        </span>
      </div>
      <p className="quiz-prompt">{current.prompt}</p>
      {current.policyName ? (
        <p className="muted quiz-meta">
          Source: {current.policyName}
          {current.section ? ` · ${current.section}` : ""}
        </p>
      ) : null}
      <div className="quiz-options">
        {current.options.map((option, optionIndex) => {
          let className = "quiz-option";
          if (showAnswer) {
            if (optionIndex === current.correctIndex) {
              className += " quiz-option-correct";
            } else if (choice === optionIndex) {
              className += " quiz-option-wrong";
            }
          } else if (choice === optionIndex) {
            className += " quiz-option-selected";
          }
          return (
            <button
              key={`${current.id}-${optionIndex}`}
              type="button"
              className={className}
              onClick={() => pickOption(optionIndex)}
              disabled={!!showAnswer}
            >
              {option}
            </button>
          );
        })}
      </div>
      {showAnswer ? (
        <p className="quiz-explanation">
          {choice === current.correctIndex ? "Correct. " : "Not quite. "}
          {current.explanation || ""}
        </p>
      ) : null}
      <div className="quiz-nav">
        <Button
          variant="secondary"
          disabled={index === 0}
          onClick={() => setIndex((value) => Math.max(0, value - 1))}
        >
          Previous
        </Button>
        <Button
          variant="secondary"
          disabled={index >= questions.length - 1}
          onClick={() => setIndex((value) => Math.min(questions.length - 1, value + 1))}
        >
          Next
        </Button>
      </div>
      {finished ? (
        <p className="quiz-score">
          Score: {score} / {questions.length}
        </p>
      ) : null}
    </div>
  );
}
