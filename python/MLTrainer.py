import pickle
from sklearn.feature_extraction.text import CountVectorizer, TfidfTransformer
from sklearn import svm
from sklearn.linear_model import LogisticRegression, SGDClassifier
from sklearn.model_selection import cross_val_score
from sklearn.metrics import recall_score
from sklearn.model_selection import cross_validate
from sklearn.pipeline import make_pipeline
from sklearn.metrics import classification_report

import random

if __name__ == '__main__':

    train_data = []
    train_target = []
    with open("../java/ML_Dataset.txt", encoding="UTF-8") as file:
        for line in file.readlines():
            sentence, score = line.split("::-::")
            train_data.append(sentence)
            train_target.append(int(score))

    random.seed(42)
    random.shuffle(train_data)
    random.seed(42)
    random.shuffle(train_target)


    #classifier = make_pipeline(CountVectorizer(), TfidfTransformer(), SGDClassifier(loss='hinge', penalty='l2', alpha=1e-3, random_state=42)) #0.77
    #classifier = make_pipeline(CountVectorizer(), TfidfTransformer(), LogisticRegression(dual=True, solver='liblinear')) #0.75
    classifier = make_pipeline(CountVectorizer(ngram_range=(1, 2)), TfidfTransformer(use_idf=False), svm.SVC(C=2, kernel='linear')) #0.79 WOO BEST SO FAR
    classifier.fit(train_data, train_target)
    scores = cross_val_score(classifier, train_data, train_target)
    print("Accuracy: %0.2f (+/- %0.2f)" % (scores.mean(), scores.std() * 2))
    pickle.dump(classifier, open('classifier.pickle', 'wb'))

    # scoring = ['precision_macro', 'recall_macro']
    # score = cross_validate(classifier, train_data, train_target, scoring=scoring, n_jobs=5, cv=5, return_train_score=False)
    # print(score)
    # pickle.dump(score, open("scores.pickle", 'wb'), protocol=pickle.HIGHEST_PROTOCOL)


    #print(classifier.predict(["He was headed for the western wing. PRP VBD VBN IN DT JJ NN . nsubjpass auxpass punct nmod case det amod"]))



