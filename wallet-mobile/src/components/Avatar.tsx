import { Image, Text, View } from 'react-native';

type AvatarProps = {
  uri?: string;
  name?: string;
  size?: number;
};

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/);
  const first = parts[0]?.[0] ?? '';
  const last = parts.length > 1 ? parts[parts.length - 1][0] : '';
  return (first + last).toUpperCase();
}

export function Avatar({ uri, name = '', size = 40 }: AvatarProps) {
  const style = { width: size, height: size, borderRadius: size / 2 };

  if (uri) {
    return <Image source={{ uri }} style={style} />;
  }

  return (
    <View className="bg-primary-fixed items-center justify-center" style={style}>
      <Text className="font-label-sm text-on-primary-fixed" style={{ fontSize: size * 0.4 }}>
        {getInitials(name)}
      </Text>
    </View>
  );
}
