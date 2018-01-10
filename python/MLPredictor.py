import pickle
import re

classifier = pickle.load(open('classifier.pickle', 'rb'))

input_data = {}
data_array = None
sentence_array = None
current_name = None
with open("../java/MLInput.txt", encoding="UTF-8") as data_file, \
        open("../java/MLSentences.txt", encoding="UTF-8") as sentence_file:
    input_sentences = sentence_file.readlines()
    for index, line in enumerate(data_file.readlines()):
        if line.startswith("###"):
            if data_array is not None:
                input_data[current_name] = (data_array, sentence_array)
            current_name = line[3:]
            data_array = []
            sentence_array = []
        elif line not in data_array:
            data_array.append(line)
            sentence_array.append(input_sentences[index])

    input_data[current_name] = (data_array, sentence_array)

for (name, data) in input_data.items():
    if len(data[0]) > 0:
        results = classifier.predict(data[0])
        descriptions = {data[1][i] for i in range(len(data[1])) if results[i] == 1}
        print(name)
        print("\n".join(descriptions))
        print("\n")





# positives = []
# negatives = []
# for index, r in enumerate(results):
#     if r == 1:
#         positives.append(index)
#     else:
#         negatives.append(index)

# in_dataset = []
#
# with open("../../IdeaProjects/EDAN70_Project/ML_Dataset.txt", "r", encoding="utf-8") as dataset:
#     for line in dataset:
#         in_dataset.append(re.split('[\.\?!]', line)[0])
#
# with open("../../IdeaProjects/EDAN70_Project/ML_Dataset.txt", "a", encoding="utf-8") as output:
#     for i in range(len(positives)):
#         positive = str(input_data[positives[i]]).strip()
#         negative = str(input_data[negatives[i]]).strip()
#         if re.split('[\.\?!]', positive)[0] not in in_dataset:
#             output.write(positive + " ::-::1\n")
#
#         if re.split('[\.\?!]', negative)[0] not in in_dataset:
#             output.write(negative + " ::-::0\n")
#         output.write(str(input_data[positives[i]]).strip() + "::-::" + str(results[positives[i]]) + "\n")
#         output.write(str(input_data[negatives[i]]).strip() + "::-::" + str(results[negatives[i]]) + "\n")
