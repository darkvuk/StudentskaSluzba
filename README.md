# Studentska Sluzba

Koristeći alat RabbitMQ implementiran je sistem u kojem postoje dvije vrste procesa:
student i studentska služba. 
- U procesu student se popunjavaju informacije o studentu: ime i prezime, broj indeksa, godina upisa i predmetu koji student prijavljuje da pohađa
naziv predmeta, semestar i studijska godina. 
- Nakon toga, ovi podaci šalju se procesu studentska služba koji provjerava da li su informacije ispravne. Provjerava se da li u
evidenciji postoji student sa datim imenom, prezimenom, brojem indeksa i godinom upisa,
kao i da li u evidenciji postoji navedeni predmet. Takođe se provjerava da li je student u
datoj studijskoj godini već prijavio taj predmet. 
- Nakon provjere, ovaj proces šalje povratnu informaciju procesu student o tome da li je prijava predmeta uspješna ili nije. Ukoliko je
prijava uspješna, ona se evidentira. 
<br />
Svi podaci koje čuva studentska služba (o
studentima, predmetima, prijavljenim ispitima) skladištite se u tekstualnim
fajlovima. Povratne poruke štampaju se na konzolama procesa student i studentska služba.

