package compilation.imp;
import java.io.*;

public class Loader {

// Codage des regles et des terminaux :

static final int max_terminaux = 33;
static final int max_regles = 7;
static final int axiome = 0;

static final String[] tab_terminaux = {
"PUSH"  ,  "POP"   ,  "ADD"   ,  "SUB"   ,  "DIV"       ,
"MUL"   ,  "INF"   ,  "SUP"   ,  "EQU"   ,  "OR"        ,
"AND"   ,  "NOT"   ,  "NEG"   ,  "INC"   ,  "DEC"       ,
"CALL"  ,  "RET"   ,  "JMP"   ,  "JC"    ,  "JNC"       ,
"WRITE" ,  "READ"  ,  "NOP"   ,  "END"   ,  "TYPE_VOID" ,
"TYPE_INTEGER"     ,  "TYPE_STRING"      ,  "ID"        ,
"NB"    ,  "CH"    ,  ":"     ,  "Vide"  ,  "Fin"};

static final String[] tab_regles = {
"Prog",
"Ligne_Code",
"Label",
"Suite_Label",
"Instr",
"Options",
"Production"};

static final String nomfich_TABLE = "tab_imp.dat";
static final int type_regle = 1;
static final int type_terminal = 2;

static DataInputStream dis;
static Liste[][] tab_analyse = new Liste[max_regles][max_terminaux];
static Liste ptr;
static Liste_prod ptr_prod;
static Liste pile_analyse;

static int code_pos(String chaine)
{
   int i;
   int code_pos_result = -1;

   i=max_terminaux-1;
   while (! ((chaine.equals(tab_terminaux[i])) || (i==0)))  i=i-1;

   code_pos_result = i;
   return code_pos_result;
}

static int regle_pos(String chaine)
{
   int i;
   int regle_pos_result = -1;

   i=max_regles-1;
   while (! ((chaine.equals(tab_regles[i])) || (i==0)))  i=i-1;
   
   regle_pos_result = i;
   return regle_pos_result;
}


static void empiler_analyse(int type_lex, int code_lex, Liste_prod ptr_lex)
{
    Liste pointeur;

   // Si ce n'est pas le Vide, on empile l'element...
   if (! ( (type_lex==type_terminal) && (code_lex==code_pos("Vide")) ))
   {
     pointeur = new Liste();
     pointeur.type_lex=type_lex;
     pointeur.code_lex=code_lex;
     pointeur.ptr_lex=ptr_lex;
     pointeur.suivant=pile_analyse;
     pile_analyse=pointeur;
   }
}


static void depiler_analyse()
{
   if (pile_analyse!=null)  pile_analyse = pile_analyse.suivant;
}



static String affiche_prod(Liste_prod ptr_lex)
{
    Liste_prod pointeur;
    String ch1,ch2;

   String affiche_prod_result;
   ch1=" ";
   pointeur=ptr_lex;
   while (! (pointeur==null))
   {
      ch2 = String.valueOf( (int) pointeur.prod_lex);
      ch1=ch1+"%"+ch2+" ";
      pointeur=pointeur.suivant;
   }
   affiche_prod_result=ch1;
   return affiche_prod_result;
}


static void charge_table()
{
    byte i,j,donnee;
    Liste pointeur;
    Liste_prod pointeur2;
    DataOutputStream dos;
    StringBuffer buffer; 
    
    try {
    dos = new DataOutputStream(new FileOutputStream("imp.log"));
    dis = new DataInputStream(new FileInputStream(nomfich_TABLE));

    for( i=0; i < max_regles; i ++)
      for( j=0; j < max_terminaux; j ++)
      {
      	donnee = dis.readByte();
      	// System.out.println("donnee = " + donnee);
                
        buffer = new StringBuffer();        
        buffer.append("Tab[" + tab_regles[i] + "," + tab_terminaux[j] + "] = ");

         if (! (donnee==0))
         {
            tab_analyse[i][j] = new Liste();
            pointeur=tab_analyse[i][j];
                      
            do {
               pointeur.type_lex = donnee;
               // System.out.println("pointeur.type_lex = " + donnee);
               donnee = dis.readByte();
               donnee = (byte) ((int)donnee - 1);
                                              
               if (pointeur.type_lex == (byte) type_regle) buffer.append(tab_regles[donnee]);
               else buffer.append(tab_terminaux[donnee]);
                            
               pointeur.code_lex=donnee;
                
               donnee = dis.readByte();
               // System.out.println("donnee = " + donnee);
                  
               if (! (donnee==0))
               {
                  pointeur.ptr_lex = new Liste_prod();
                  pointeur2=pointeur.ptr_lex;
                  pointeur2.prod_lex=donnee;

                  donnee = dis.readByte();
                  // System.out.println("donnee = " + donnee);
                  
                  while (! (donnee==0))
                  {
                     pointeur2.suivant = new Liste_prod();
                     pointeur2=pointeur2.suivant;
                     pointeur2.prod_lex=donnee;
                     donnee = dis.readByte();
                     // System.out.println("donnee = " + donnee);
                  }
                  pointeur2.suivant=null;
               }
               else pointeur.ptr_lex=null;

               buffer.append(affiche_prod(pointeur.ptr_lex));

               donnee = dis.readByte();
               // System.out.println("donnee = " + donnee);
               
               if (! (donnee==0))
               {
                  pointeur.suivant = new Liste();
                  pointeur=pointeur.suivant;
               }
               else pointeur.suivant=null;
            } while (!( (donnee==0) ));
         }
         else tab_analyse[i][j]=null;
         
        dos.writeBytes(buffer.toString()+"\n");
        buffer = null;
      }

   dis.close();
   dos.close();
   
   }
   catch (Exception e ) {
   System.out.println(e);
   }
}

	public Loader () {
	}

	public static void main(String[] args) {
		Loader.charge_table();
	}
}
