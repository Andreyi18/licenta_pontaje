import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
} from "react";
import type { User, LoginRequest } from "../types";
import { UserRole } from "../types";
import { authApi, setAuthToken, getAuthToken } from "../api/api";

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => void;
  hasRole: (roles: UserRole | UserRole[]) => boolean;
  updateUser: (userData: User) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // verificam daca utilizatorul are rolul necesar
  const hasRole = useCallback(
    (roles: UserRole | UserRole[]): boolean => {
      if (!user) return false;
      const roleArray = Array.isArray(roles) ? roles : [roles];
      return roleArray.includes(user.role);
    },
    [user],
  );

  // initializam starea de autentificare din tokenul stocat
  useEffect(() => {
    const initAuth = async () => {
      const token = getAuthToken();
      if (token) {
        try {
          const userData = await authApi.getCurrentUser();
          setUser(userData);
        } catch {
          // token invalid, il stergem
          setAuthToken(null);
        }
      }
      setIsLoading(false);
    };
    initAuth();
  }, []);

  // functia de login
  const login = async (credentials: LoginRequest) => {
    const response = await authApi.login(credentials);
    const userData: User = {
      id: response.userId,
      email: response.email,
      firstName: response.firstName,
      lastName: response.lastName,
      fullName: `${response.firstName} ${response.lastName}`,
      role: response.role,
      status: "ACTIVE" as any,
      departmentName: response.departmentName,
      departmentId: response.departmentId,
      createdAt: "",
      updatedAt: "",
    };
    setUser(userData);
  };

  // functia de logout
  const logout = () => {
    authApi.logout();
    setUser(null);
  };

  // dam update la user data local
  const updateUser = (userData: User) => {
    setUser(userData);
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user,
    isLoading,
    login,
    logout,
    hasRole,
    updateUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// hook pentru a folosi contextul de autentificare
export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};

export default AuthContext;
