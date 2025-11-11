package compilation.imp;
import java.io.*;

/* Interpreteur pour Machine � Pile */

public class IMP {

final int max_id = 100;
final int max_codes = 200;
final int max_res = 27;
final int type_echec = -1;

final int _instr = 1;
final int _donnee = 2;
final int _adr = 3;
final int _chaine = 4;
final int _valeur = 5;

final String nomfich_SOURCE = "prgs/jeu.obj";

public class Id {
	public String nom;
	public int adr;

	public Id() {
	}

	public Id(String _nom, int _adr) {
		nom = _nom;
		adr = _adr;
	}

}

public class Code {
   int type_code = -1;

   /*  Instruction  */
   String instr;

   /*  Donnee       */
   int type_donnee;
   int adr;
   String chaine;
   int valeur;

   public Code() {
   }
}

public class Pile1 {
   Pile1 suivant = null;
   String chaine;
   int type_donnee;
   int valeur;

   public Pile1() {
   }
}

public class Pile2 {
   Pile2 suivant = null;
   int co;

   public Pile2() {
   }
}

final String[] tab_res = {
"PUSH"  ,  "POP"   ,  "ADD"   ,  "SUB"   ,  "DIV"       ,
"MUL"   ,  "INF"   ,  "SUP"   ,  "EQU"   ,  "OR"        ,
"AND"   ,  "NOT"   ,  "NEG"   ,  "INC"   ,  "DEC"       ,
"CALL"  ,  "RET"   ,  "JMP"   ,  "JC"    ,  "JNC"       ,
"WRITE" ,  "READ"  ,  "NOP"   ,  "END"   ,  "TYPE_VOID" ,
"TYPE_INTEGER"     ,  "TYPE_STRING"
};

File fichier_SOURCE;
DataInputStream dis;
RandomAccessFile raf;
DataOutputStream dos;

int pos_fichier;

Id[] tab_id = new Id[max_id];
Code[] memoire = new Code[max_codes];

int pos_id,co,op1,op2,i;

Pile1 pile_travaille;
Pile2 pile_controle;

boolean erreur;

int code,valeur;
String chaine;

/*  Gestion des Erreurs:  */

void type_erreur(int numero)
{
   erreur=true;
   switch (numero) {
      case 0: System.out.println("Erreur #0: Erreur Lexicale"); break;
      case 1: System.out.println("Erreur #1: Erreur Syntaxique"); break;
      case 2: System.out.println("Erreur #2: Identificateur Inconnu"); break;
      case 3: System.out.println("Erreur #3: Identificateur Duplique"); break;
      case 4: System.out.println("Erreur #4: Instruction Inconnu"); break;
   }
}

/*  Structure de listes associatives:  */

int code_id(String chaine)
{
   int i;
   int code_id_result = -1;

   i = max_id-1;
   while (! ((i==-1) || (chaine.equals(tab_id[i].nom)) ))  i=i-1;
   code_id_result = i;

   return code_id_result;
}

int mot_res(String chaine)
{
   int i;
   int mot_res_result;

   i = max_res-1;
   while (! ((i==-1) || (chaine.equals(tab_res[i])) ))  i=i-1;

   if (i == -1)  mot_res_result = Loader.code_pos("ID");
   else 	 mot_res_result = i;

   return mot_res_result;
}


void push()
{
   Pile1 pointeur;

   pointeur = new Pile1();
   pile_controle.co += 1;
   // System.out.println("pile_controle.co = " + pile_controle.co);
   {
      Code with = memoire[pile_controle.co];
      // System.out.println("type_donnee = " + with.type_donnee);
      switch (with.type_donnee) {
      case _adr: {
		       // System.out.println("with.adr " + with.adr);
               if (memoire[with.adr].type_donnee == _valeur)
               {
                  pointeur.type_donnee = _valeur;
                  pointeur.valeur = memoire[with.adr].valeur;
                  // System.out.println("push " + memoire[with.adr].valeur);
               }
               else
               {
                  pointeur.type_donnee = _chaine;
                  pointeur.chaine = memoire[with.adr].chaine;
                  // System.out.println("push " + memoire[with.adr].chaine);
               }
            }
            break;
      case _chaine: {
                  pointeur.type_donnee = _chaine;
                  pointeur.chaine = with.chaine;
                  // System.out.println("push " + memoire[pile_controle.co].chaine);
               }
               break;
      case _valeur: {
                  pointeur.type_donnee = _valeur;
                  pointeur.valeur = with.valeur;
                  // System.out.println("push " + memoire[pile_controle.co].valeur);
               }
               break;
      }
   }
   pointeur.suivant = pile_travaille;
   pile_travaille = pointeur;
}

void pop()
{
   Pile1 pointeur;
   int i;

   pile_controle.co += 1;
   // System.out.println("pile_controle.co = " + pile_controle.co);
   i = memoire[pile_controle.co].adr;
   // System.out.println("memoire[pile_controle.co].adr = " + i);
   {
      Pile1 with = pile_travaille;

      if (with.type_donnee == _valeur)  {
      	memoire[i].valeur = with.valeur;
      }
      else {
      	memoire[i].chaine = with.chaine;
      }
   }

   pointeur = pile_travaille.suivant;
   pile_travaille = pointeur;
}

void empiler1(int op)
{
   Pile1 pointeur;

   pointeur = new Pile1();
   pointeur.type_donnee = _valeur;
   pointeur.valeur = op;
   pointeur.suivant = pile_travaille;
   pile_travaille = pointeur;
}

int depiler1()
{
   Pile1 pointeur;
   int i;

   int depiler1_result;
   i = pile_travaille.valeur;
   pointeur = pile_travaille.suivant;
   pile_travaille = pointeur;
   depiler1_result = i;
   return depiler1_result;
}

void empiler2(int adr)
{
   Pile2 pointeur;

   pointeur = new Pile2();
   pointeur.co = adr;
   pointeur.suivant = pile_controle;
   pile_controle = pointeur;
}

void depiler2()
{
   Pile2 pointeur;

   pointeur = pile_controle.suivant;
   pile_controle = pointeur;
}

/* ---------------------- Analyse Lexicale -------------------------- */

boolean lettre(char car)
/* lettre -> A..Z | a..z | _ */
{
   boolean lettre_result;

   if ( ((car>='A') && (car<='Z'))
	|| ((car>='a') && (car<='z'))
	|| (car == '_') || (car == '.')) lettre_result=true;
   else lettre_result=false;

   return lettre_result;
}

boolean chiffre(char car)
/* chiffre -> Hexadecimal */
{
   boolean chiffre_result;
   switch (car) {
      case '0': case '1': case '2': case '3': case '4' : case '5':
      case '6': case '7': case '8': case '9': chiffre_result=true;
      break;

      default: chiffre_result = false;
   }
   return chiffre_result;
}

boolean signe(char car)
/* Signe -> + | - */
{
   boolean signe_result;
   switch (car) {
      case '+':case '-' : signe_result=true; break;
      default:      signe_result=false;
   }
   return signe_result;
}

// boolean blanc(int pos_fichier) throws IOException
boolean blanc() throws IOException
/* blanc -> #32 | #10 | #13 */
{
   char car;
   boolean blanc_result;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      switch (car) {
         case '\40':
         case '\12':
         case '\15': {pos_fichier=pos_fichier+1; blanc_result=true;} break;

         default: blanc_result=false;
      }
   }
   catch (IOException ioe) {
      throw ioe;
   }

   return blanc_result;
}

// boolean depoint(int pos_fichier) throws IOException
boolean depoint() throws IOException
/* depoint -> : */
{
   char car;

   boolean depoint_result;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      switch (car) {
         case ':': {pos_fichier=pos_fichier+1; depoint_result=true;} break;
         default:  depoint_result=false;
      }
   }
   catch (IOException ioe) {
      throw ioe;
   }

   return depoint_result;
}

// boolean id2(int pos_fichier, String chaine) throws IOException
boolean id2() throws IOException
/* id2 -> lettre id2 | chiffre id2 | vide */
{
   char car;
   boolean id2_result;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (lettre(car) || chiffre(car))
      {
         pos_fichier = pos_fichier + 1;
         chaine = chaine + Character.toUpperCase(car);

         if (chaine.length() <= 16)  id2_result = id2(); // id2(pos_fichier,chaine);
         else id2_result = false;
      }
      else id2_result = true;
   }
   catch (IOException ioe) {
      throw ioe;
   }

   return id2_result;
}

// boolean id1(int pos_fichier, String chaine) throws IOException
boolean id1() throws IOException
/* id1 -> lettre id1 */
{
   char car;
   boolean id1_result;
   chaine="";

   // System.out.println("id1 : pos_fichier = " + pos_fichier);
   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (lettre(car))
      {
         pos_fichier = pos_fichier + 1;
         chaine = chaine + Character.toUpperCase(car);
         id1_result = id2(); // id2(pos_fichier,chaine);
      }
      else id1_result = false;
   }
   catch (IOException ioe) {
      throw ioe;
   }

   // if (id1_result == true) System.out.println("chaine = " + chaine);
   return id1_result;
}

// boolean nb2(int pos_fichier, int valeur) throws IOException
boolean nb2() throws IOException
/* nb2 -> chiffre nb2 | vide */
{
   char car;
   boolean nb2_result;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (chiffre(car))
      {
         pos_fichier = pos_fichier + 1;

         valeur = valeur * 10 + Character.getNumericValue(car);

         if (valeur <= 65535)  nb2_result = nb2(); // nb2(pos_fichier,valeur);
         else nb2_result = false;
      }
      else nb2_result = true;
   }
   catch (IOException ioe) {
      throw ioe;
   }

   return nb2_result;
}

// boolean nb1(int pos_fichier, int valeur) throws IOException
boolean nb1() throws IOException
/* nb1 -> chiffre nb2 */
{
   char car;
   boolean nb1_result;
   valeur = 0;

   // System.out.println("nb1 : pos_fichier = " + pos_fichier);

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (chiffre(car))
      {
         pos_fichier = pos_fichier + 1;

         valeur = valeur + Character.getNumericValue(car);

         nb1_result = nb2(); // nb2(pos_fichier,valeur);
      }
      else nb1_result = false;
   }
   catch (IOException ioe) {
      throw ioe;
   }

   // if (nb1_result == true) System.out.println("valeur = " + valeur);
   return nb1_result;
}

boolean chaine1() throws IOException
/* chaine1 -> ' car ' */
{
   char car;
   int sortie;
   boolean chaine1_result;
   chaine = "";

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (car=='\47')
      {
         sortie=0;
         do {
            pos_fichier=pos_fichier+1;
            raf.seek(pos_fichier);
            car = (char) raf.readUnsignedByte();

            if (car == '\47')  sortie = 1;
            else if (chaine.length() == 255) sortie = 2;
            else chaine = chaine + car;
         } while (!( sortie != 0));

         pos_fichier = pos_fichier + 1;
         if (sortie == 1)  chaine1_result = true;
         else chaine1_result = false;
      }
      else chaine1_result = false;
   }
   catch (IOException ioe) {
      throw ioe;
   }

   // if (chaine1_result == true) System.out.println("chaine = " + chaine);
   return chaine1_result;
}

// boolean commentaire(int pos_fichier) throws IOException
boolean commentaire() throws IOException
/*
   commentaire -> { car }
                 #123 car #125
   = parenthese
 */
{
   char car;
   int sortie;
   boolean parenthese_result;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (car == '{')
      {
         sortie=0;
         do {
            pos_fichier = pos_fichier + 1;
            raf.seek(pos_fichier);
            car = (char) raf.readUnsignedByte();

            if (car == '}')  sortie = 1;
         } while (!(sortie != 0));

         pos_fichier = pos_fichier + 1;
         if (sortie == 1)  parenthese_result = true;
         else parenthese_result = false;
      }
      else parenthese_result = false;
   }
   catch (IOException ioe) {
      sortie = 2;
      throw ioe;
   }

   return parenthese_result;
}


// void lex(int pos_fichier, int code, int valeur, String chaine)
void lex()
{
   try {
      while (blanc() || commentaire());
      // System.out.println("pos_fichier = " + pos_fichier);

      if (id1())		{code = mot_res(chaine);}
      else if (nb1())		{code = Loader.code_pos("NB");}
      else if (chaine1())	{code = Loader.code_pos("CH");}
      else if (depoint())	{code = Loader.code_pos(":");}
      else 			code = type_echec;
      // System.out.println("code = " + code + " " + Loader.tab_terminaux[code]);
   }
   catch (IOException ioe) {
      code=Loader.code_pos("Fin");
   }
}

void affiche_lex()
{
   do {
      // lex(pos_fichier,code,valeur,chaine);
      lex();

      System.out.print(Loader.tab_terminaux[code]);

      if (code==Loader.code_pos("ID"))  System.out.print(" " + chaine);
      if (code==Loader.code_pos("NB"))  System.out.print(" " + valeur);
      if (code==Loader.code_pos("CH"))  System.out.print(" " + chaine);

      System.out.println();
   } while (!( (code==Loader.code_pos("Fin")) || (code==0)));

   if (code==0)  type_erreur(0);
}

/*------------------ Productions du code assembleur ---------------------*/

void prod_0()
{
   /* Initialisation */
   co = 1;
   pos_id = 0;
}

void prod_1()
{
   int present;

   /* Id -> Tab_Id */
   present = code_id(chaine);
   if (present == -1)
   {
     pos_id = pos_id + 1;
     tab_id[pos_id] = new Id();
     tab_id[pos_id].nom = chaine;
     tab_id[pos_id].adr = co;
   }
   else type_erreur(3);
}

void prod_2()
{
   co += 1;
}

void prod_3()
{
   memoire[co].type_code = _instr;
   memoire[co].instr = chaine;
   // System.out.println("instr = " + chaine);
}

void prod_4()
{
   memoire[co].type_code = _donnee;
   memoire[co].type_donnee = _adr;
}

void prod_5()
{
   // memoire[co] = new Code();
   memoire[co].type_code = _donnee;
   memoire[co].type_donnee = _chaine;
   memoire[co].chaine = new String("");
}

void prod_6()
{
   // memoire[co] = new Code();
   memoire[co].type_code = _donnee;
   memoire[co].type_donnee = _valeur;
   memoire[co].valeur = 0;
}

void prod_7()
{
   int present;

   present = code_id(chaine);
   if (! (present == -1))  memoire[co].adr = tab_id[present].adr;
   else type_erreur(2);
}

void prod_8()
{
   // System.out.println(co + " : " + chaine);
   memoire[co].chaine = chaine;
}

void prod_9()
{
   // System.out.println(co + " : " + valeur);
   memoire[co].valeur = valeur;
}

// -------------------------------------------------------------------------------


void produire_code(int passe)
{
   Liste_prod pointeur;

   pointeur = Loader.pile_analyse.ptr_lex;
   while (! (pointeur == null))
   {
      if (passe == 1)
      switch (pointeur.prod_lex) {
         case 1: prod_1(); break;
         case 2: prod_2(); break;
      }

      if (passe==2)
      switch (pointeur.prod_lex) {
         case 2: prod_2(); break;
         case 3: prod_3(); break;
         case 4: prod_4(); break;
         case 5: prod_5(); break;
         case 6: prod_6(); break;
         case 7: prod_7(); break;
         case 8: prod_8(); break;
         case 9: prod_9(); break;
      }

      pointeur=pointeur.suivant;
   }
}

/*
----------------------- Analyse Syntaxique --------------------------
Algorithme de l'analyse :
Soit X le symbole en sommet de pile
et a le symbole d'entree courant

1: Si X=a DEPILER X
2: Si X est un non-terminal, consulter Table[X,a]
   c'est une Erreur --> Echec
   sinon DEPILER X , EMPILER Table[X,a]
   Ex: Table[X,a]=( X --> U V W )
   on DEPILE X , on EMPILE dans l'ordre W V U
   de maniere a avoir U en haut de la pile.
   (le travaille est facilite avec une table
   d'analyse inversee...)

On avance pos_fichier sur le symbole suivant,
et on recommence.
----------------------------------------------------------------------
*/

void analyse(int Passe)
{
   Liste pointeur;

   Loader.pile_analyse = null;
   Loader.empiler_analyse(Loader.type_terminal,	Loader.code_pos("Fin"),	null);
   Loader.empiler_analyse(Loader.type_regle,	Loader.axiome,		null);

   // lex(pos_fichier,code,valeur,chaine);
   lex();

   if (code == type_echec)  type_erreur(0);

   if (Loader.pile_analyse == null) System.out.println("pile_analyse is null");

   while (!( ((Loader.pile_analyse.code_lex == Loader.code_pos("Fin"))
               && (code == Loader.code_pos("Fin"))) || erreur))
   {
      /* X est un terminal (Code) */

      if (Loader.pile_analyse.type_lex == Loader.type_terminal)
         if (Loader.pile_analyse.code_lex == code)
         {
            // System.out.println("X est un terminal");
            produire_code(Passe);

            /* Regle1 */
            Loader.depiler_analyse();

            // lex(pos_fichier,code,valeur,chaine);
	    lex();

            if (code == type_echec)  type_erreur(0);
         }
         else type_erreur(1);
      else

      /* X est un non-terminal (Regle) */

      {
         // System.out.println("X est un non-terminal (regle) ");
         // System.out.println("Tab[ " + Loader.tab_regles[Loader.pile_analyse.code_lex] + " , " + Loader.tab_terminaux[code] + " ]");

         pointeur = Loader.tab_analyse[Loader.pile_analyse.code_lex][code];
         if (pointeur == null) System.out.println("pointeur null");

         if (! (pointeur == null))
         {
            produire_code(Passe);

            /* Regle2 */
            Loader.depiler_analyse();
            while (! (pointeur == null))
            {
               Loader.empiler_analyse(pointeur.type_lex,
                               pointeur.code_lex,
                               pointeur.ptr_lex);
               pointeur=pointeur.suivant;
            }
         }
         else type_erreur(1);
      }
   }
}

public IMP()
{
   try {
      fichier_SOURCE = new File(nomfich_SOURCE);
      raf = new RandomAccessFile(fichier_SOURCE,"r");

      erreur = false;
      for( i=0; i < max_id; i ++)
      {
	     tab_id[i] = new Id("",0);
         // tab_id[i].nom = "";
         // tab_id[i].adr = 0;
      }

      for( i=0; i < max_codes; i ++)
      {
	     memoire[i] = new Code();
      }


      System.out.println("Interpreteur pour Machine a Pile >");
      System.out.println("Code Source: " + nomfich_SOURCE);
      System.out.println();

      Loader.charge_table();

      System.out.println("Compilation en code machine >");

      System.out.println("Premiere passe> ");
      prod_0(); /* Initialisation */
      pos_fichier=0;
      analyse(1);

   /*
   for( i=0; i < max_id; i ++)
   {
	 if (!tab_id[i].nom.equals("")) {
	    System.out.println("i = " + i);
	    System.out.println("nom = " + tab_id[i].nom);
	    System.out.println("adr = " + tab_id[i].adr);

         }
   }
   */

      System.out.println("Deuxieme passe> ");
      prod_0(); // Initialisation
      pos_fichier=0;
      analyse(2);

      if (!(erreur))  System.out.println("Succes");
      else	      System.out.println("Erreur");

      raf.close();
   }
   catch (Exception e) {
      System.out.println("Exception " + e);
   }

   /*
   for( i=0; i < max_codes; i ++)
   {
	 if (memoire[i].type_code != -1) {
	    System.out.println("adr = " + i + " --------------------");

	    if ( memoire[i].type_code == _instr) System.out.println(memoire[i].instr);
	    System.out.println("adr = " + memoire[i].adr);
	    if (memoire[i].type_donnee == _valeur) System.out.println("valeur = " + memoire[i].valeur);
            else System.out.println("chaine = " + memoire[i].chaine);
         }
   }
   */

   /* Interpretation: ------------------------------------------------- */

   pile_travaille = null;
   pile_controle = new Pile2();
   pile_controle.co = 1;

   while (! ( ((pile_controle.co == max_codes) || erreur)
           || (memoire[pile_controle.co].instr.equals("END"))))
   {
           Code with = memoire[pile_controle.co];
           // System.out.println("code = " + with.instr);

           if (with.instr.equals("PUSH"))
           {
              push();
              pile_controle.co += 1;
           }
      else if (with.instr.equals("POP"))
           {
              pop();
              pile_controle.co += 1;
           }
      else if (with.instr.equals("ADD"))
           {
              op2 = depiler1();
              op1 = depiler1();
              op1 = op1 + op2;
              empiler1(op1);
              pile_controle.co += 1;
           }
      else if (with.instr.equals("SUB"))
           {
              op2 = depiler1();
              op1 = depiler1();
              op1 = op1 - op2;
              empiler1(op1);
              pile_controle.co += 1;
           }
      else if (with.instr.equals("DIV"))
           {
              op2 = depiler1();
              op1 = depiler1();
              op1 = op1 / op2;
              empiler1(op1);
              pile_controle.co += 1;
           }
      else if (with.instr.equals("MUL"))
           {
              op2 = depiler1();
              op1 = depiler1();
              op1 = op1 * op2;
              empiler1(op1);
              pile_controle.co += 1;
           }
      else if (with.instr.equals("INF"))
           {
              op2 = depiler1();
              op1 = depiler1();
              if (op1 < op2)  op1 = 1;
              else op1 = 0;
              empiler1(op1);
              pile_controle.co += 1;
           }
      else if (with.instr.equals("SUP"))
           {
              op2 = depiler1();
              op1 = depiler1();
              if (op1 > op2)  op1 = 1;
              else op1 = 0;
              empiler1(op1);
              pile_controle.co += 1;
           }
      else if (with.instr.equals("EQU"))
           {
              op2 = depiler1();
              op1 = depiler1();
              if (op1 == op2)  op1 = 1;
              else op1 = 0;
              empiler1(op1);
              pile_controle.co += 1;
           }
      else if (with.instr.equals("OR"))
           {
              op2 = depiler1();
              op1 = depiler1();
              if ( (op1 != 0) || (op2 != 0) )  op1 = 1;
              else op1 = 0;
              empiler1(op1);
              pile_controle.co += 1;
           }
      else if (with.instr.equals("AND"))
           {
              op2 = depiler1();
              op1 = depiler1();
              if ( (op1 != 0) && (op2 != 0) ) op1 = 1;
              else op1 = 0;
              empiler1(op1);
              pile_controle.co += 1;
           }
      else if (with.instr.equals("NOT"))
           {
              op1 = depiler1();
              if (op1 == 0)  op1 = 1;
              else op1 = 0;
              empiler1(op1);
              pile_controle.co += 1;
           }
      else if (with.instr.equals("NEG"))
           {
              op1 = depiler1();
              op1 = -op1;
              empiler1(op1);
              pile_controle.co += 1;
           }
      else if (with.instr.equals("INC"))
           {
              op1 = depiler1();
              op1 += 1;
              empiler1(op1);
              pile_controle.co += 1;
           }
      else if (with.instr.equals("DEC"))
           {
              op1 = depiler1();
              op1 -= 1;
              empiler1(op1);
              pile_controle.co += 1;
           }
      else if (with.instr.equals("CALL"))
           {
              pile_controle.co += 1;
              i=memoire[pile_controle.co].adr;
              pile_controle.co += 1;
              empiler2(i);
           }
      else if (with.instr.equals("RET"))
           {
              depiler2();
           }
      else if (with.instr.equals("JMP"))
           {
              pile_controle.co += 1;
              pile_controle.co = memoire[pile_controle.co].adr;
           }
      else if (with.instr.equals("JC"))
           {
              pile_controle.co += 1;
              op1 = depiler1();
              if (op1 != 0)
              pile_controle.co = memoire[pile_controle.co].adr;
              else pile_controle.co += 1;
           }
      else if (with.instr.equals("JNC"))
           {
              pile_controle.co += 1;
              op1 = depiler1();
              if (op1 == 0) {
              	 pile_controle.co = memoire[pile_controle.co].adr;
              }
              else pile_controle.co += 1;
           }
       else if (with.instr.equals("WRITE"))
            {
               pile_controle.co += 1;
               op1 = memoire[pile_controle.co].adr;
               if (memoire[op1].type_donnee == _valeur) System.out.println(memoire[op1].valeur);
               else System.out.println(memoire[op1].chaine);
               pile_controle.co += 1;
            }
       else if (with.instr.equals("READ"))
            {
               pile_controle.co += 1;
               op1 = memoire[pile_controle.co].adr;
               try {
				  byte[] buffer = new byte[80];
				  String read = "";

				  if (System.in.read(buffer) != -1) read = (new String(buffer)).trim();

                  if (memoire[op1].type_donnee == _valeur) {
					  memoire[op1].valeur = Integer.parseInt(read);
			          // System.out.println("valeur = " + memoire[op1].valeur);
			      }
                  else {
					  memoire[op1].chaine = read;
			      	  // System.out.println("chaine = " + memoire[op1].chaine);
			      }
               }
               catch (Exception e) {
                  System.out.println(e);
               }
               pile_controle.co += 1;
            }
       else if (with.instr.equals("NOP"))
            pile_controle.co += 1;
       else if (with.instr.equals("END"))  ;
       else type_erreur(4);
   }

}

static public void main(String[] args) {
   IMP imp = new IMP();
}

}