export type RoleName = 'ADMIN' | 'CLIENT';

export interface Role { id: number; libelle: RoleName; }

export interface Categorie { id?: number; libelle: string; description?: string | null; }

export interface Produit {
  id?: number;
  nom: string;
  marque: string;
  modele?: string | null;
  prix: number;
  description?: string | null;
  dureeGarantieStandard: number;
  photo?: string | null;
  categorie: Categorie;
}

export interface User {
  id: number;
  email: string;
  nom: string;
  prenom: string;
  tel?: string | null;
  photoProfil?: string | null;
  enabled: boolean;
  roles: Role[];
}

export type StatutGarantie = 'ACTIVE' | 'BIENTOT_EXPIREE' | 'EXPIREE' | 'INCONNU';

export interface Achat {
  id: number;
  dateAchat: string;            // yyyy-MM-dd
  prixPaye: number;
  magasin?: string | null;
  numeroSerie?: string | null;
  dureeGarantie: number;
  dateFinGarantie: string;      // yyyy-MM-dd
  facturePdf: string;
  photoAchat?: string | null;
  user: User;
  produit: Produit;
  statutGarantie: StatutGarantie;
  joursRestants: number;
}

export type StatutReclamation = 'OUVERTE' | 'EN_COURS' | 'RESOLUE' | 'REFUSEE';

export interface Reclamation {
  id: number;
  dateOuverture: string;
  motif: string;
  description?: string | null;
  statut: StatutReclamation;
  justificatifPdf?: string | null;
  reponseAdmin?: string | null;
  achat: Achat;
}

export interface Session {
  token: string;
  id: number;
  email: string;
  nom: string;
  prenom: string;
  roles: RoleName[];
  photoProfil?: string | null;
}

export interface RegisterPayload { email: string; password: string; nom: string; prenom: string; tel?: string; }

export interface Message {
  id: number;
  clientId: number;
  deAdmin: boolean;            // true : écrit par l'équipe
  contenu: string;
  dateEnvoi: string;           // yyyy-MM-ddTHH:mm:ss
  lu: boolean;
  reclamationId?: number | null;
}

export interface Conversation {
  clientId: number;
  nom: string;
  prenom: string;
  email: string;
  photoProfil?: string | null;
  dernierMessage: string;
  dernierDeAdmin: boolean;
  dateDernierMessage: string;
  nonLus: number;
}
